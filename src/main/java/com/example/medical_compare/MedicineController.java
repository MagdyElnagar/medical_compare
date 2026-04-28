package com.example.medical_compare;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.medical_compare.Service.ZeroStockService;
import com.example.medical_compare.model.Product;
import com.example.medical_compare.model.product_rebak;

import jakarta.servlet.http.HttpServletRequest;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

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
		
		
		model.addAttribute("products", products);
		
		
		

		return "zerostock";
	}

	@PostMapping("/delete_sginal_ZeroSotck_rebak")
	public String delete_sginal_ZeroSotck_rebak(@RequestParam Long id,@RequestParam String name, @RequestParam String price, Model model,
			HttpServletRequest request) {

		System.out.println("🔥 Delete REQUEST 🔥");
		System.out.println("prod_name = " + name);
		System.out.println("price = " + price);

		System.out.println("URL = " + request.getRequestURI());

		zss.delete(id);

		List<Product> products = zss.findAll();
		
		
		model.addAttribute("products", products);

		return "zerostock";
	}


	@PostMapping("/zerostockadd")
	public String add(@RequestParam String name,
	                  @RequestParam double price,
	                  @RequestParam double discount) {
	    System.out.println("🔥 ADD REQUEST 🔥");

	    Product p = new Product();
	    p.setName(name);
	    p.setPrice(price);
	    p.setDiscount(discount);

	    zss.save(p);

	    return "redirect:/zerostock";
	}

	@GetMapping("/test")
	public String test() {
		return "test";
	}

	
	@GetMapping("/view-data")
	public String viewData(Model model) {
		model.addAttribute("medicines", repository.findAll());
		return "view-data";
	}

	@GetMapping("/upload_zero")
	public String upload_zero() {

		return "upload_zero";
	}

	@GetMapping("/clear")
	public String clearData() {
		repository.deleteAll();
		return "redirect:/upload";
	}

	private String getCellValue(Cell cell) {

		if (cell == null)
			return "";

		switch (cell.getCellType()) {

		case STRING:
			return cell.getStringCellValue().trim();

		case NUMERIC:
			return String.valueOf((int) cell.getNumericCellValue());

		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());

		default:
			return "";
		}
	}

	@PostMapping("/upload_zero")
	public String upload_zero(@RequestParam("file") MultipartFile file, Model model) {

		List<Medicine> allMedicines = repository.findAll();

		// 👇 هنا بقى بنقرأ الإكسل مباشرة
		List<Medicine> allZeroMedicines = service.loadFromExcel(file);

		List<String> warehouses = allMedicines.stream().map(Medicine::getWarehouse).distinct().toList();

		Map<String, ComparisonRow> comparisonMap = new LinkedHashMap<>();

		for (Medicine zeroMed : allZeroMedicines) {

			Medicine matched = service.findBestMatch(zeroMed.getBrandName(), allMedicines);

			if (matched == null)
				continue;

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

				if (med.getWarehouse() == null)
					continue;

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

	private String findExactMatch(Map<String, ComparisonRow> map, String name, String strength, Double price) {
		for (Map.Entry<String, ComparisonRow> entry : map.entrySet()) {
			ComparisonRow existing = entry.getValue();

			boolean priceMatch = price != null && existing.getPrice() != null
					&& Math.abs(existing.getPrice() - price) < 0.001;

			boolean nameMatch = existing.getBrandName().equalsIgnoreCase(name);

			boolean strengthMatch = (existing.getStrength() == null && strength == null)
					|| (existing.getStrength() != null && strength != null
							&& existing.getStrength().equalsIgnoreCase(strength));

			if (priceMatch && nameMatch && strengthMatch) {
				return entry.getKey();
			}
		}
		return null;
	}

	private String findMatchByAndName(Map<String, ComparisonRow> map, String name, String strength) {
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();

		for (String key : map.keySet()) {
			ComparisonRow existing = map.get(key);

			// الشرط الأول: السعر متطابق تماماً
			boolean isSamePrice = true;

			if (isSamePrice) {
				// الشرط الثاني: الاسم متشابه جداً (أكثر من 85%) بنفس السعر
				double score = jw.apply(existing.getBrandName().toLowerCase(), name.toLowerCase());
				if (score > 0.85) {
					return key;
				}

			}
		}
		return null;
	}

}
