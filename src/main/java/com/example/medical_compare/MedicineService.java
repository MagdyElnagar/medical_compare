package com.example.medical_compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MedicineService {

	public String cleanMedicineName(String name) {
		if (name == null)
			return "";

		// 1. تطبيع الأرقام العربية للإنجليزية
		name = name.replaceAll("٠", "0").replaceAll("١", "1").replaceAll("٢", "2")
				.replaceAll("٣", "3").replaceAll("٤", "4").replaceAll("٥", "5")
				.replaceAll("٦", "6").replaceAll("٧", "7").replaceAll("٨", "8")
				.replaceAll("٩", "9").trim();

		// 2. تطبيع الحروف العربية
		name = name.replaceAll("[أإآ]", "ا")
				.replace("ة", "ه")
				.replace("ى", "ي")
				.replace("ـ", "")
				.trim();

		// 3. إزالة رموز الضوضاء
		name = name.replaceAll("[#\\*\\.\\-،؛@/\\\\()\\[\\]®]", " ");

		// 4. إزالة أسماء الشركات والموردين
		Map<String, String> replacements = new HashMap<>();
		replacements.put("نوفارتيس", "");
		replacements.put("اوتسوكا", "");
		replacements.put("اتسوكا", "");
		replacements.put("ايبيكو", "");
		replacements.put("ابيكو", "");
		replacements.put("امون", "");
		replacements.put("ازيس", "");
		replacements.put("ايفا فارم", "");
		replacements.put("ايفا", "");
		replacements.put("فاركو", "");
		replacements.put("المصريه", "");
		replacements.put("الفرعونيه", "");
		replacements.put("يونيفارما", "");
		replacements.put("لينكو فارم", "");
		replacements.put("انترفارم", "");
		replacements.put("الاوروبيه", "");
		replacements.put("العامريه", "");
		replacements.put("وول فارم", "");
		replacements.put("فايزر", "");
		replacements.put("بارك فيل", "");
		replacements.put("اكديما", "");
		replacements.put("دراج فارم", "");
		replacements.put("ابوت", "");
		replacements.put("جلاكسو", "");
		replacements.put("سانوفى", "");
		replacements.put("باير", "");
		replacements.put("روش", "");
		replacements.put("ليلى", "");
		replacements.put("كيبر", "");
		replacements.put("سيجما", "");
		replacements.put("مارسيل", "");
		replacements.put("راميدا", "");
		replacements.put("الاسكندريه", "");
		replacements.put("كيما", "");
		replacements.put("المهن", "");
		replacements.put("مينافارم", "");
		replacements.put("مينا فارم", "");

		// 5. إزالة كلمات الحالة والوصف
		replacements.put("استلام ", "");
		replacements.put("شركات ", "");
		replacements.put("فقط", "");
		replacements.put("احترس", "");
		replacements.put("شريط", "");
		replacements.put("ممنوعه", "");
		replacements.put("صغيره", "");
		replacements.put("علبه", "");
		replacements.put("كبيره", "");
		replacements.put("تشغيله", "");
		replacements.put("القاهره", "");
		replacements.put("بالضمان", "");
		replacements.put("ضمان", "");
		replacements.put("س ج", "");
		replacements.put("س.ج", "");
		replacements.put("س  ج", "");
		replacements.put("س ق", "");
		replacements.put("سعر", "");
		replacements.put("جديد", "");
		replacements.put("قديم", "");
		replacements.put("كبير", "");
		replacements.put("صغير", "");
		replacements.put("عادى", "");

		// 6. توحيد أشكال الجرعة
		replacements.put("اقراص", "قرص");
		replacements.put("قراص", "قرص");
		replacements.put("كبسوله", "قرص");
		replacements.put("كبسول", "قرص");
		replacements.put("نقط", "قطره");
		replacements.put("ممتد المفعول", "اكس ار");
		replacements.put("اس ار", "اكس ار");
		replacements.put("فوار", "اكياس");
		replacements.put("حبيبات", "اكياس");
		replacements.put(" بلس", " plus ");
		replacements.put(" بلاس", " plus ");
		replacements.put(" بلاص", " plus ");

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			name = name.replace(entry.getKey(), entry.getValue());
		}

		// 7. إزالة patterns الضوضاء
		name = name.replaceAll("(?i)(باكو|باكت|كرتونه)\\s*\\d+", " ")
				.replaceAll("\\.xlsx|\\.xls", " ")
				.replaceAll("(?i)(احترس|ركز|جنية|جنيه)\\s*\\d+", " ")
				.replaceAll("\\s*/(\\d{4,}|\\S+)?\\s*$", "")
				.replaceAll("-{2,}", " ")
				.trim();

		// 8. *** تطبيع تكرار الحروف ***
		// ريموواكس => ريمواكس ، اووميبازول => اوميبازول
		name = name.replaceAll("(.)\\1+", "$1");

		// 9. إزالة أرقام الباكت المنفردة في نهاية الاسم
		name = name.replaceAll("\\s*\\d+\\s*(ق|شريط|انبول|امبول)\\b", " ");

		// 10. تنظيف المسافات النهائية
		name = name.replaceAll("\\s+", " ").trim();

		// 11. توحيد كبسول -> قرص (بعد كل التنظيف)
		name = name.replaceAll("كبسول", "قرص").trim();
		name = name.replaceAll("\\s+", " ").trim();

		return name;
	}

	/**
	 * بيأخذ اسم الملف ويرجع اسم المخزن نظيف بدون امتداد
	 * مثال: "الشمس.xlsx" => "الشمس"
	 */
	public String cleanWarehouseName(String filename) {
		if (filename == null) return "unknown";
		// شيل الامتداد
		filename = filename.replaceAll("(?i)\\.xlsx?$", "").trim();
		// شيل أي underscores أو dashes
		filename = filename.replace("_", " ").replace("-", " ").trim();
		return filename;
	}

	public Medicine parseExcelRow(String rawName, Double price, Double discount, String warehouse) {
		Medicine med = new Medicine();
		med.setRawName(rawName);
		med.setPrice(price);
		med.setDiscount(discount);
		med.setWarehouse(warehouse);
		med.setBrandName(rawName);
		return med;
	}

	/**
	 * بيستخرج التركيز من اسم الدواء كـ string للمقارنة
	 * ايراستابكس 40مجم       => "40"
	 * ايراستابكس كو 40-5مجم  => "40-5"
	 * ايراستابكس تريو 40-5-12.5 => "40-5-12.5"
	 * ريمواكس 15             => "15"
	 * بدون تركيز             => ""
	 */
	public String extractDose(String name) {
		if (name == null) return "";
		// ابحث عن pattern: رقم أو أرقام مفصولة بـ - أو /
		// مثال: 40 | 40-5 | 40-5-12.5 | 20/10
		java.util.regex.Matcher m = java.util.regex.Pattern
			.compile("(\\d+(?:[.,]\\d+)?(?:[\\-/]\\d+(?:[.,]\\d+)?)*)")
			.matcher(name);
		StringBuilder doses = new StringBuilder();
		while (m.find()) {
			if (doses.length() > 0) doses.append("-");
			doses.append(m.group(1).replace(",", "."));
		}
		return doses.toString();
	}

	public double similarity(String s1, String s2) {
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
		return jw.apply(s1, s2);
	}

	public Medicine findBestMatch(String name, List<Medicine> allMedicines) {
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
		double bestScore = 0;
		Medicine bestMatch = null;

		for (Medicine med : allMedicines) {
			if (med.getBrandName() == null)
				continue;
			double score = jw.apply(cleanMedicineName(med.getBrandName()), cleanMedicineName(name));
			if (score > bestScore) {
				bestScore = score;
				bestMatch = med;
			}
		}
		return bestScore >= 0.80 ? bestMatch : null;
	}

	public List<Medicine> loadFromExcel(MultipartFile file) {
		List<Medicine> list = new ArrayList<>();
		try {
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				String name = row.getCell(0).getStringCellValue();
				if (name.isEmpty())
					continue;
				Medicine med = new Medicine();
				med.setBrandName(name);
				list.add(med);
			}
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}