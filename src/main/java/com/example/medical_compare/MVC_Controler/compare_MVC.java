package com.example.medical_compare.MVC_Controler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.medical_compare.ComparisonRow;
import com.example.medical_compare.Medicine;
import com.example.medical_compare.MedicineRepository;
import com.example.medical_compare.MedicineService;
import com.example.medical_compare.model.Product;

@Controller
public class compare_MVC {

	@Autowired
	private MedicineRepository repository;
	@Autowired
	private MedicineService service;

	@GetMapping("/upload")
	public String upload() {
		return "upload";
	}

	@PostMapping("/upload")
	public String uploadMultipleExcel(@RequestParam("files") MultipartFile[] files) throws Exception {
		for (MultipartFile file : files) {
			if (file.isEmpty())
				continue;

			String warehouseName = service.cleanMedicineName(file.getOriginalFilename());

			// 1. مسح البيانات القديمة لهذا المخزن فقط قبل الرفع الجديد
			repository.deleteByWarehouse(warehouseName);

			// 2. معالجة الملف كما فعلنا سابقاً
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);

			List<Medicine> medicines = new ArrayList<>();
			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				try {
					// قراءة البيانات (تأكد من ترتيب الأعمدة عندك: اسم، سعر، خصم)
					
					String name = row.getCell(0).getStringCellValue();
					double price = row.getCell(1).getNumericCellValue();
					double discount = row.getCell(2).getNumericCellValue();
					discount = ((int) discount);
					// System.out.println(name);
					medicines.add(
							service.parseExcelRow(service.cleanMedicineName(name), price, discount, warehouseName));
				} catch (Exception e) {
					// سطر خاطئ، أكمل الباقي
				}
			}
			// 3. حفظ البيانات الجديدة للمخزن
			repository.saveAll(medicines);
			workbook.close();
		}
		return "redirect:/comparison";
	}

	@GetMapping("/comparison")
	public String showComparison(Model model) {
		List<Medicine> allMedicines = repository.findAll();
		List<Medicine> allZeroMedicines = null;

		List<String> warehouses = allMedicines.stream().map(Medicine::getWarehouse).distinct()
				.collect(Collectors.toList());

		// نستخدم LinkedHashMap للحفاظ على ترتيب الإدخال
		Map<String, ComparisonRow> comparisonMap = new LinkedHashMap<>();

		for (Medicine med : allMedicines) {
			// 1. تنظيف الاسم (البراند والتركيز)
			String cleanName = service.cleanMedicineName(med.getBrandName());
			String strength = med.getStrength() != null ? med.getStrength() : "";
			double price = med.getPrice();

			// 2. البحث عن صنف موجود بنفس السعر ونفس الاسم (تقريباً)
			String bestKey = findMatchByPriceAndName(comparisonMap, cleanName, strength, price);

			if (bestKey != null) {
				// صنف مطابق في السعر والاسم -> ندمج الخصم
				comparisonMap.get(bestKey).getWarehouseDiscounts().put(med.getWarehouse(), med.getDiscount());
			} else {
				// صنف جديد تماماً (سعر مختلف أو اسم بعيد)
				ComparisonRow row = new ComparisonRow();
				row.setBrandName(med.getBrandName());
				row.setStrength(strength);
				row.setPrice(price);
				row.getWarehouseDiscounts().put(med.getWarehouse(), med.getDiscount());

				// المفتاح الجديد يحتوي على السعر لضمان عدم تداخل الأسعار المختلفة
				String newKey = price + "_" + cleanName + "_" + strength;
				comparisonMap.put(newKey, row);
			}
		}

		model.addAttribute("warehouses", warehouses);
		model.addAttribute("comparisonRows", comparisonMap.values());
		return "comparison";
	}

	private String findMatchByPriceAndName(Map<String, ComparisonRow> map, String name, String strength, double price) {
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();

		for (String key : map.keySet()) {
			ComparisonRow existing = map.get(key);

			// الشرط الأول: السعر متطابق تماماً
			boolean isSamePrice = Math.abs(existing.getPrice() - price) < 0.01;

			if (isSamePrice) {
				// الشرط الثاني: الاسم متشابه جداً (أكثر من 85%) بنفس السعر
				double score = jw.apply(existing.getBrandName().toLowerCase(), name.toLowerCase());
				if (score > 0.90) {
					return key;
				}

			}
		}
		return null;
	}
}
