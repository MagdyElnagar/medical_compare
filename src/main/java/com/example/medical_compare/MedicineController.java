package com.example.medical_compare;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.medical_compare.Repository.ZeroStockMatchRepository;
import com.example.medical_compare.Service.ReferenceProductService;
import com.example.medical_compare.Service.ZeroStockService;
import com.example.medical_compare.model.Product;
import com.example.medical_compare.model.ReferenceProduct;
import com.example.medical_compare.model.ZeroStockWithMatches;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MedicineController {

	@Autowired
	private MedicineRepository repository;

	@Autowired
	private ZeroStockMatchRepository zeroStockMatchRepo;

	@Autowired
	private MedicineService service;
	@Autowired
	private ZeroStockService zss;
	@Autowired
	private ReferenceProductService referenceProductService;

	@GetMapping("/delete_reference_prices")
	public String deleteReferencePrices() {
		System.out.println("Ref Work");

		referenceProductService.deleteAll();

		return "redirect:/zerostock";

	}

	@GetMapping("/zerostock")
	public String zerostock(Model model) {
		List<Product> products = zss.findAll();
		List<Medicine> allMedicines = repository.findAll();

		List<String> warehouses = allMedicines.stream().map(Medicine::getWarehouse).filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());
		List<ZeroStockWithMatches> allMatches = zeroStockMatchRepo.findAll();

		// ② Map: productId → (warehouse → discount)
		Map<Long, Map<String, Double>> matchMap = new LinkedHashMap<>();
		for (ZeroStockWithMatches m : allMatches) {
			matchMap.computeIfAbsent(m.getProductId(), k -> new LinkedHashMap<>()).merge(m.getWarehouseName(),
					m.getDiscount(), Math::max);
		}

		// ③ بناء الـ enrichedList
		List<ZeroStockWithMatches> enrichedList = new ArrayList<>();
		for (Product p : products) {
			Map<String, Double> matches = matchMap.getOrDefault(p.getId(), Map.of());
			enrichedList.add(new ZeroStockWithMatches(p, matches));
		}

		model.addAttribute("enrichedList", enrichedList);
		model.addAttribute("warehouses", warehouses);
		return "zerostock";
	}

	@PostMapping("/upload_reference_prices")
	public String uploadReferencePrices(@RequestParam("fileRef") MultipartFile file) {
		try {
			referenceProductService.deleteAll();
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);
			List<ReferenceProduct> list = new ArrayList<>();

			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				Cell codeCell = row.getCell(0);
				Cell nameCell = row.getCell(1);
				Cell priceCell = row.getCell(2);
				if (codeCell == null || priceCell == null)
					continue;

				String itemCode = getCellString(codeCell).trim();
				String name = nameCell != null ? nameCell.getStringCellValue().trim() : "";
				double price = 0;
				try {
					price = priceCell.getNumericCellValue();
				} catch (Exception ignored) {
				}

				if (itemCode.isEmpty())
					continue;

				ReferenceProduct rp = new ReferenceProduct();
				rp.setItemCode(itemCode);
				rp.setName(name);
				rp.setPrice(price);
				list.add(rp);
			}
			referenceProductService.saveAll(list);
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/zerostock";
	}

	private Map<String, Double> findWarehouseMatches(String name, List<Medicine> allMedicines) {
		Map<String, Double> result = new LinkedHashMap<>();
		if (name == null || name.isBlank())
			return result;
		String cleanName = service.cleanMedicineName(name);
		for (Medicine med : allMedicines) {
			if (med.getBrandName() == null || med.getWarehouse() == null)
				continue;
			double score = service.similarity(service.cleanMedicineName(med.getBrandName()), cleanName);
			if (score >= 0.95) {
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
			// ① جيب الـ reference prices بالكود
			Map<String, Double> priceMap = referenceProductService.findAll().stream()
					.collect(Collectors.toMap(ReferenceProduct::getItemCode, ReferenceProduct::getPrice, (a, b) -> a));

			// ② جيب أدوية المخازن مرة واحدة + clean أسماءها مرة واحدة
			List<Medicine> allMedicines = repository.findAll();
			Map<Medicine, String> cleanedMeds = new LinkedHashMap<>();
			for (Medicine m : allMedicines) {
				if (m.getBrandName() != null && m.getWarehouse() != null) {
					cleanedMeds.put(m, service.cleanMedicineName(m.getBrandName()));
				}
			}

			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);
			List<Product> toSave = new ArrayList<>();
			List<ZeroStockWithMatches> matchesToSave = new ArrayList<>();

			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				Cell codeCell = row.getCell(0);
				Cell nameCell = row.getCell(1);
				if (codeCell == null)
					continue;

				String itemCode = getCellString(codeCell).trim();
				String name = nameCell != null ? nameCell.getStringCellValue().trim() : "";
				if (itemCode.isEmpty())
					continue;

				// ③ السعر بالكود — O(1)
				double price = priceMap.getOrDefault(itemCode, 0.0);

				Product p = new Product();
				p.setItemCode(itemCode);
				p.setName(service.cleanMedicineName(name));
				p.setPrice(price);
				toSave.add(p);

				// ④ الـ fuzzy مع المخازن — بس هنا مرة واحدة
				String cleanName = service.cleanMedicineName(name);
				Map<String, Double> warehouseMatches = new LinkedHashMap<>();
				for (Map.Entry<Medicine, String> entry : cleanedMeds.entrySet()) {
					double score = service.similarity(entry.getValue(), cleanName);
					if (score >= 0.88) {
						warehouseMatches.merge(entry.getKey().getWarehouse(), entry.getKey().getDiscount(), Math::max);
					}
				}

				// ⑤ حفظ النتائج — هنحتاج الـ id بعد الـ save
				p.setPendingMatches(warehouseMatches); // هنضيف الحقل ده مؤقتاً
			}

			// ⑥ save الـ products وبعدين save الـ matches بالـ id
			zss.saveAll(toSave);
			zeroStockMatchRepo.deleteAll();

			for (Product p : toSave) {
				p.getPendingMatches().forEach((warehouse, discount) -> {
					ZeroStockWithMatches m = new ZeroStockWithMatches();
					m.setProductId(p.getId());
					m.setWarehouseName(warehouse);
					m.setDiscount(discount);
					matchesToSave.add(m);
				});
			}
			zeroStockMatchRepo.saveAll(matchesToSave);
			workbook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/zerostock";
	}

	@PostMapping("/delete_sginal_ZeroSotck_rebak")
	public String deleteSingleZeroStock(@RequestParam Long id, @RequestParam String name, @RequestParam String price,
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

	@PostMapping("/upload_zero")
	public String upload_zero(@RequestParam("file") MultipartFile file, Model model) {
		List<Medicine> allMedicines = repository.findAll();
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

	private String getCellString(Cell cell) {
		if (cell == null)
			return "";
		return switch (cell.getCellType()) {
		case STRING -> cell.getStringCellValue().trim();
		case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
		default -> "";
		};
	}

}