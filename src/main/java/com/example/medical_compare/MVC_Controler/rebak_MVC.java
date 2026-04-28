package com.example.medical_compare.MVC_Controler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.medical_compare.Service.ProductRebakService;
import com.example.medical_compare.model.product_rebak;

@Controller
public class rebak_MVC {

    @Autowired
    private ProductRebakService productRebakService;

    @PostMapping("/deleteAllRebak")
    public String deleteAllRebak() {
        productRebakService.deleteAll();
        return "redirect:/rebak";
    }

    @GetMapping("/rebak")
    public String rebak(Model model) {
        List<product_rebak> products = productRebakService.findAll();
        model.addAttribute("products", products);
        return "rebak";
    }

    @PostMapping("/delete_sginal_rebak")
    public String delete_sginal_rebak(@RequestParam Long id, Model model) {
        productRebakService.deleteById(id);
        List<product_rebak> products = productRebakService.findAll();
        model.addAttribute("products", products);
        return "rebak";
    }

    @PostMapping("/rebak_add")
    public String saveProduct(@RequestParam String prod_name, @RequestParam String store, Model model) {
        product_rebak p = new product_rebak();
        p.setProdName(prod_name);
        p.setStore(store);
        productRebakService.save(p);
        List<product_rebak> products = productRebakService.findAll();
        model.addAttribute("products", products);
        return "rebak";
    }
}
