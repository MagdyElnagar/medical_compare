package com.example.medical_compare.MVC_Controler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Cell;
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

			String warehouseName = service.cleanWarehouseName(file.getOriginalFilename());

			// مسح البيانات القديمة لهذا المخزن فقط
			repository.deleteByWarehouse(warehouseName);

			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);

			List<Medicine> medicines = new ArrayList<>();
			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				try {
					String name     = row.getCell(0).getStringCellValue();
					double price    = row.getCell(1).getNumericCellValue();
					double discount = (int) row.getCell(2).getNumericCellValue();

					medicines.add(
						service.parseExcelRow(
							service.cleanMedicineName(name),
							price,
							discount,
							warehouseName
						)
					);
				} catch (Exception e) {
					// سطر خاطئ، أكمل
				}
			}
			repository.saveAll(medicines);
			workbook.close();
		}
		return "redirect:/comparison";
	}

	@GetMapping("/comparison")
	public String showComparison(Model model) {
		List<Medicine> allMedicines = repository.findAll();

		List<String> warehouses = allMedicines.stream()
				.map(Medicine::getWarehouse)
				.distinct()
				.collect(Collectors.toList());

		Map<String, ComparisonRow> comparisonMap = new LinkedHashMap<>();

		for (Medicine med : allMedicines) {
			String cleanName = service.cleanMedicineName(med.getBrandName());
			String strength  = med.getStrength() != null ? med.getStrength() : "";
			double price     = med.getPrice();

			String bestKey = findMatchByPriceAndName(comparisonMap, cleanName, price);

			if (bestKey != null) {
				comparisonMap.get(bestKey).getWarehouseDiscounts()
						.put(med.getWarehouse(), med.getDiscount());
			} else {
				ComparisonRow row = new ComparisonRow();
				row.setBrandName(med.getBrandName());
				row.setStrength(strength);
				row.setPrice(price);
				row.getWarehouseDiscounts().put(med.getWarehouse(), med.getDiscount());

				String newKey = price + "_" + cleanName + "_" + strength;
				comparisonMap.put(newKey, row);
			}
		}

		model.addAttribute("warehouses", warehouses);
		model.addAttribute("comparisonRows", comparisonMap.values());
		return "comparison";
	}

	/**
	 * المنطق:
	 * 1. السعر لازم يتطابق تماماً (فرق أقل من 0.01)
	 * 2. التركيز (الأرقام في الاسم) لازم يتطابق — ده يمنع دمج ايراستابكس 40 مع ايراستابكس كو 40-5
	 * 3. الاسم بعد شيل التركيز لازم يكون متشابه بـ 75%+
	 */
	private String findMatchByPriceAndName(Map<String, ComparisonRow> map,
			String name, double price) {

		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
		String bestKey   = null;
		double bestScore = 0;

		// استخرج التركيز من الاسم الجديد
		String newDose = service.extractDose(name);

		for (String key : map.keySet()) {
			ComparisonRow existing = map.get(key);

			// 1. السعر لازم يكون متطابق تقريباً
			if (Math.abs(existing.getPrice() - price) > 0.01)
				continue;

			// 2. التركيز لازم يتطابق تماماً
			String existingDose = service.extractDose(
				service.cleanMedicineName(existing.getBrandName())
			);
			if (!newDose.equals(existingDose))
				continue;

			// 3. الاسم بعد شيل التركيز لازم يكون متشابه
			double score = jw.apply(
				service.cleanMedicineName(existing.getBrandName()),
				name
			);

			if (score > 0.75 && score > bestScore) {
				bestScore = score;
				bestKey   = key;
			}
		}
		return bestKey;
	}
}