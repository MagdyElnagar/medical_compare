package com.example.medical_compare.MVC_Controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.medical_compare.MedicineRepository;
import com.example.medical_compare.MedicineService;


@Controller
public class discount_MVC {
	
	
	
	@Autowired
	private MedicineRepository repository;
	@Autowired
	private MedicineService service;
	
	
	
	@GetMapping("/find_discount")
	public String find_discount() {

		return "find_discount";
	}
	
	
	@PostMapping("/find_discount_method")
	public String find_discount(@RequestParam("old_qty") int old_qty, @RequestParam("old_price") int old_price,
			@RequestParam("old_discount") double old_discount, @RequestParam("new_qty") int new_qty,
			@RequestParam("new_price") int new_price, @RequestParam("new_discount") double new_discount, Model model) {

		Double old_cost_per_1 = old_price * (1 - old_discount / 100);
		Double old_cost_for_all = old_cost_per_1 * old_qty;

		Double new_cost_per_1 = new_price * (1 - new_discount / 100);
		Double new_cost_for_all = new_cost_per_1 * new_qty;

		int all_QTY = old_qty + new_qty;
		double all_cost = new_cost_for_all + old_cost_for_all;
		double cost_per_1 = all_cost / all_QTY;
		double last_discount = (old_price - cost_per_1) / old_price * 100;

		// نبعت القيم للـ HTML
		model.addAttribute("old_qty", old_qty);

		model.addAttribute("old_price", old_price);

		model.addAttribute("old_discount", old_discount);

		model.addAttribute("new_qty", new_qty);

		model.addAttribute("new_discount", new_discount);

		model.addAttribute("new_price", new_price);

		model.addAttribute("all_QTY", all_QTY);
		model.addAttribute("last_discount", last_discount);
		model.addAttribute("cost_per_1", cost_per_1);

		return "find_discount"; // نفس الصفحة
	}


}
