package com.example.medical_compare;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.medical_compare.Service.ZeroStockService;
import com.example.medical_compare.model.Product;
import com.example.medical_compare.model.ZeroStockWithMatches;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MedicineController {

	@Autowired
	private MedicineRepository repository;
	@Autowired
	private MedicineService service;
	@Autowired
	private ZeroStockService zss;

	@GetMapping("/zerostock")
	public String zerostock(Model model) {
		List<Product> products = zss.findAll();
		List<Medicine> allMedicines = repository.findAll();

		List<String> warehouses = allMedicines.stream()
				.map(Medicine::getWarehouse)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		List<ZeroStockWithMatches> enrichedList = new ArrayList<>();
		for (Product p : products) {
			Map<String, Double> matches = findWarehouseMatches(p.getName(), allMedicines);
			enrichedList.add(new ZeroStockWithMatches(p, matches));
		}

		model.addAttribute("enrichedList", enrichedList);
		model.addAttribute("warehouses", warehouses);
		return "zerostock";
	}

	private Map<String, Double> findWarehouseMatches(String name, List<Medicine> allMedicines) {
		Map<String, Double> result = new LinkedHashMap<>();
		if (name == null || name.isBlank()) return result;
		String cleanName = service.cleanMedicineName(name);
		for (Medicine med : allMedicines) {
			if (med.getBrandName() == null || med.getWarehouse() == null) continue;
			double score = service.similarity(
				service.cleanMedicineName(med.getBrandName()), cleanName
			);
			if (score >= 0.90) {
				result.merge(med.getWarehouse(), med.getDiscount(), Math::max);
			}
		}
		return result;
	}

	// إضافة صنف واحد يدوي - اسم بس
	@PostMapping("/zerostockadd")
	public String add(@RequestParam String name) {
	    Product p = new Product();
	    p.setName(name);
	    zss.save(p);
	    return "redirect:/zerostock";
	}

	// رفع Excel فيه أسماء المصفرات
	@PostMapping("/zerostock_upload_excel")
	public String uploadZeroStockExcel(@RequestParam("file") MultipartFile file) {
		try {
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				if (row.getRowNum() == 0) continue; // skip header
				Cell cell = row.getCell(0);
				if (cell == null) continue;
				String name = cell.getStringCellValue().trim();
				if (name.isEmpty()) continue;
				Product p = new Product();
				p.setName(name);
				zss.save(p);
			}
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/zerostock";
	}

	@PostMapping("/delete_sginal_ZeroSotck_rebak")
	public String deleteSingleZeroStock(@RequestParam Long id,
			@RequestParam String name,
			@RequestParam String price,
			HttpServletRequest request) {
		zss.delete(id);
		return "redirect:/zerostock";
	}

	// مسح كل المصفرات
	@PostMapping("/deleteAllZeroStock")
	public String deleteAllZeroStock() {
		zss.deleteAll();
		return "redirect:/zerostock";
	}

	@GetMapping("/test")
	public String test() { return "test"; }

	@GetMapping("/view-data")
	public String viewData(Model model) {
		model.addAttribute("medicines", repository.findAll());
		return "view-data";
	}

	@GetMapping("/upload_zero")
	public String upload_zero() { return "upload_zero"; }

	@GetMapping("/clear")
	public String clearData() {
		repository.deleteAll();
		return "redirect:/upload";
	}

	@PostMapping("/upload_zero")
	public String upload_zero(@RequestParam("file") MultipartFile file, Model model) {
		List<Medicine> allMedicines = repository.findAll();
		List<Medicine> allZeroMedicines = service.loadFromExcel(file);
		List<String> warehouses = allMedicines.stream().map(Medicine::getWarehouse).distinct().toList();

		Map<String, ComparisonRow> comparisonMap = new LinkedHashMap<>();

		for (Medicine zeroMed : allZeroMedicines) {
			Medicine matched = service.findBestMatch(zeroMed.getBrandName(), allMedicines);
			if (matched == null) continue;

			String cleanName = service.cleanMedicineName(matched.getBrandName());
			String strength = matched.getStrength() != null ? matched.getStrength() : "";
			Double price = matched.getPrice();
			String key = price + "_" + cleanName + "_" + strength;

			comparisonMap.putIfAbsent(key, new ComparisonRow());
			ComparisonRow row = comparisonMap.get(key);
			row.setBrandName(matched.getBrandName());
			row.setStrength(strength);
			row.setPrice(price);

			for (Medicine med : allMedicines) {
				if (med.getWarehouse() == null) continue;
				double score = service.similarity(service.cleanMedicineName(med.getBrandName()), cleanName);
				if (score >= 0.85) {
					row.getWarehouseDiscounts().put(med.getWarehouse(), med.getDiscount());
				}
			}
		}

		model.addAttribute("warehouses", warehouses);
		model.addAttribute("comparisonRows", comparisonMap.values());
		return "upload_zero";
	}
}