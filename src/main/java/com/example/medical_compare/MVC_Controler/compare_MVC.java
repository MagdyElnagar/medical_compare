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
	 * كلمات التمييز — لو صنفين فيهم كلمتين مختلفتين من القائمة دي
	 * يبقوا منتجين مختلفين حتى لو الاسم متشابه والسعر واحد
	 * مثال: اوتريفين اطفال vs اوتريفين كبار -> مختلفين
	 *        اوروفكس فراوله vs اوروفكس قرنفل -> مختلفين
	 */
	private static final java.util.Set<String> DISCRIMINATING_WORDS = new java.util.HashSet<>(
		java.util.Arrays.asList(
			// فئات المرضى
			"اطفال", "كبار", "بيبي", "اطفل", "نساء", "رجال",
			// نكهات وعطور
			"فراوله", "نعناع", "قرنفل", "ليمون", "برتقال", "توت", "عود", "ورد",
			"لافندر", "النسيم", "البينك", "هيربال", "فيتامين", "فانيلا",
			// ألوان
			"ابيض", "اسود", "رصاصي", "احمر", "ازرق", "اخضر", "بنى",
			// تركيبات مختلفة
			"بلس", "plus", "فورت", "ادفانس", "ماكس", "اكسترا", "سوبر",
			// مناطق الجسم
			"للانف", "للعين", "للفم", "للاذن", "للجسم", "للشعر", "للوجه"
		)
	);

	private boolean hasDifferentDiscriminatingWord(String name1, String name2) {
		java.util.Set<String> words1 = new java.util.HashSet<>();
		java.util.Set<String> words2 = new java.util.HashSet<>();
		for (String w : DISCRIMINATING_WORDS) {
			if (name1.contains(w)) words1.add(w);
			if (name2.contains(w)) words2.add(w);
		}
		// لو الاثنين فيهم كلمات تمييز مختلفة -> منتجات مختلفة
		if (!words1.isEmpty() && !words2.isEmpty() && !words1.equals(words2))
			return true;
		return false;
	}

	/**
	 * المنطق:
	 * 1. السعر لازم يتطابق تماماً
	 * 2. مفيش كلمات تمييز مختلفة (اطفال vs كبار، فراوله vs قرنفل)
	 * 3. التركيز لازم يتطابق
	 * 4. الاسم متشابه بـ 75%+
	 */
	private String findMatchByPriceAndName(Map<String, ComparisonRow> map,
			String name, double price) {

		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
		String bestKey   = null;
		double bestScore = 0;

		String newDose = service.extractDose(name);

		for (String key : map.keySet()) {
			ComparisonRow existing = map.get(key);

			// 1. السعر لازم يكون متطابق
			if (Math.abs(existing.getPrice() - price) > 0.01)
				continue;

			String existingClean = service.cleanMedicineName(existing.getBrandName());

			// 2. لو فيه كلمات تمييز مختلفة -> مش نفس المنتج
			if (hasDifferentDiscriminatingWord(name, existingClean))
				continue;

			// 3. التركيز لازم يتطابق
			String existingDose = service.extractDose(existingClean);
			if (!newDose.equals(existingDose))
				continue;

			// 4. الاسم متشابه
			double score = jw.apply(existingClean, name);
			if (score > 0.75 && score > bestScore) {
				bestScore = score;
				bestKey   = key;
			}
		}
		return bestKey;
	}
}