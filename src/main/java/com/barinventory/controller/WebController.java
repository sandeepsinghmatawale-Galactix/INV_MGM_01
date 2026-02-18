package com.barinventory.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.barinventory.entity.Bar;
import com.barinventory.entity.BarProductPrice;
import com.barinventory.entity.InventorySession;
import com.barinventory.entity.InventorySessionDTO;
import com.barinventory.entity.Product;
import com.barinventory.entity.StockroomInventory;
import com.barinventory.repository.InventorySessionRepository;
import com.barinventory.service.BarService;
import com.barinventory.service.InventorySessionService;
import com.barinventory.service.PricingService;
import com.barinventory.service.ProductService;
import com.barinventory.service.ReportService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final BarService barService;
    private final ProductService productService;
    private final PricingService pricingService;
    private final InventorySessionService sessionService;
    private final ReportService reportService;
    private final InventorySessionRepository sessionRepo;

    // ================= HOME =================

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("bars", barService.getAllActiveBars());
        return "index";
    }

    // ================= BARS =================

    @GetMapping("/bars")
    public String listBars(Model model) {
        model.addAttribute("bars", barService.getAllActiveBars());
        return "bars/list";
    }

    @GetMapping("/bars/new")
    public String newBarForm() {
        return "bars/form";
    }

    @PostMapping("/bars/new")
    public String createBar(@RequestParam String barName,
                            @RequestParam String location,
                            @RequestParam(required = false) String contactNumber,
                            @RequestParam(required = false) String ownerName) {

        Bar bar = Bar.builder()
                .barName(barName)
                .location(location)
                .contactNumber(contactNumber)
                .ownerName(ownerName)
                .active(true)
                .build();

        barService.createBar(bar);
        return "redirect:/";
    }

    // ================= PRODUCTS =================

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllActiveProducts());
        return "products/list";
    }

    @GetMapping("/products/new")
    public String newProductForm() {
        return "products/form";
    }

    @PostMapping("/products/new")
    public String createProduct(@RequestParam String productName,
                                @RequestParam String category,
                                @RequestParam(required = false) String brand,
                                @RequestParam(required = false) String volumeML,
                                @RequestParam String unit) {

        Product product = Product.builder()
                .productName(productName)
                .category(category)
                .brand(brand)
                .volumeML(volumeML != null && !volumeML.isEmpty()
                        ? new BigDecimal(volumeML) : null)
                .unit(unit)
                .active(true)
                .build();

        productService.createProduct(product);
        return "redirect:/products";
    }

    // ================= PRICING =================

    @GetMapping("/pricing/{barId}")
    public String managePricing(@PathVariable Long barId, Model model) {
        Bar bar = barService.getBarById(barId);
        model.addAttribute("bar", bar);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("prices", pricingService.getPricesByBar(barId));
        return "pricing/manage";
    }

    @PostMapping("/pricing/{barId}/{productId}")
    public String savePrice(@PathVariable Long barId,
                            @PathVariable Long productId,
                            @RequestParam String sellingPrice,
                            @RequestParam(required = false) String costPrice) {

        BarProductPrice price = BarProductPrice.builder()
                .sellingPrice(new BigDecimal(sellingPrice))
                .costPrice(costPrice != null && !costPrice.isEmpty()
                        ? new BigDecimal(costPrice) : BigDecimal.ZERO)
                .active(true)
                .build();

        pricingService.setPrice(barId, productId, price);
        return "redirect:/pricing/" + barId;
    }

    // ================= SESSIONS =================

    @GetMapping("/sessions/{barId}")
    public String listSessions(@PathVariable Long barId, Model model) {
        model.addAttribute("sessions", sessionService.getSessionsByBar(barId));
        model.addAttribute("bar", barService.getBarById(barId));
        return "list";
    }

    @GetMapping("/sessions/{barId}/new")
    public String newSessionForm(@PathVariable Long barId, Model model) {
        model.addAttribute("bar", barService.getBarById(barId));
        return "new";
    }

    @PostMapping("/sessions/{barId}/new")
    public String createSession(@PathVariable Long barId,
                                @RequestParam String shiftType,
                                @RequestParam(required = false) String notes) {

        InventorySession inv = sessionService.initializeSession(barId, shiftType, notes);
        return "redirect:/stockroom/" + inv.getSessionId();
    }

    // ================= STOCKROOM =================

    @GetMapping("/stockroom/{sessionId}")
    public String viewStockroom(@PathVariable Long sessionId, Model model) {
        InventorySession inv = sessionService.getSession(sessionId);
        List<Product> products = productService.getAllActiveProducts();

        if (products.isEmpty()) {
            model.addAttribute("error", "No active products found. Please add products first.");
        }

        model.addAttribute("inv", inv);           // ← "inv" not "session"
        model.addAttribute("products", products);
        return "stockroom";
    }

    @PostMapping("/stockroom/{sessionId}")
    public String saveStockroom(@PathVariable Long sessionId,
                                @RequestParam Map<String, String> formData,
                                Model model) {
        try {
            InventorySession inv = sessionService.getSession(sessionId);
            List<Product> products = productService.getAllActiveProducts();
            List<StockroomInventory> inventories = new ArrayList<>();

            for (Product product : products) {
                String opening  = formData.get("opening_"  + product.getProductId());
                String received = formData.get("received_" + product.getProductId());
                String closing  = formData.get("closing_"  + product.getProductId());
                String remarks  = formData.get("remarks_"  + product.getProductId());

                if (opening != null || received != null || closing != null) {
                    StockroomInventory inventory = StockroomInventory.builder()
                            .session(inv)
                            .product(product)
                            .openingStock(parseDecimal(opening))
                            .receivedStock(parseDecimal(received))
                            .closingStock(parseDecimal(closing))
                            .remarks(remarks)
                            .build();
                    inventories.add(inventory);
                }
            }

            sessionService.saveStockroomInventory(sessionId, inventories);
            sessionService.createDistributionRecords(sessionId);
            return "redirect:/sessions/distribution/" + sessionId;

        } catch (Exception e) {
            InventorySession inv = sessionService.getSession(sessionId);
            model.addAttribute("inv", inv);
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("error", e.getMessage());
            return "stockroom";
        }
    }

    // ================= DISTRIBUTION =================

  
    @GetMapping("/sessions/distribution/{sessionId}")
    public String distributionPage(@PathVariable Long sessionId, Model model) {
        InventorySession inv = sessionService.getSession(sessionId);

        if (inv == null) throw new RuntimeException("Session not found: " + sessionId);
        if (inv.getBar() == null) throw new RuntimeException("Bar not mapped to session: " + sessionId);

        model.addAttribute("inv", inv);
        model.addAttribute("bar", inv.getBar());          // ← add bar separately
        model.addAttribute("sessionId", sessionId);       // ← add sessionId separately
        model.addAttribute("distributions",
                inv.getDistributionRecords() != null ? inv.getDistributionRecords() : List.of());
        return "distribution";
    }

    // ================= WELLS =================

    @GetMapping("/sessions/wells/{sessionId}")
    public String wellsPage(@PathVariable Long sessionId, Model model) {
        InventorySession inv = sessionService.getSession(sessionId);
        Bar bar = inv.getBar();

        Map<Long, BarProductPrice> prices = pricingService.getPriceMapForBar(bar.getBarId());
        Map<Long, BigDecimal> distributionMap = sessionService.getDistributionMapForSession(sessionId);

        model.addAttribute("inv", inv);           // ← "inv" not "session"
        model.addAttribute("bar", bar);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("barId", bar.getBarId());
        model.addAttribute("prices", prices);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("distributionMap", distributionMap);
        model.addAttribute("wellNames", Arrays.asList("BAR_1", "BAR_2", "SERVICE_BAR"));
        return "wells";
    }

    // ================= WELL SAVE =================

    @PostMapping("/sessions/wells/{sessionId}/save")
    @ResponseBody
    public ResponseEntity<?> saveWellInventory(
            @PathVariable Long sessionId,
            @RequestParam Map<String, String> formData) {
        try {
            sessionService.saveWellInventoryFromForm(sessionId, formData);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= COMMIT =================

    @PostMapping("/api/sessions/{sessionId}/commit")
    public ResponseEntity<?> commitSession(@PathVariable Long sessionId) {
        try {
            sessionService.commitSession(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= REPORTS =================

    @GetMapping("/reports/{barId}/daily")
    public String dailyReport(@PathVariable Long barId,
                              @RequestParam(required = false) String date,
                              Model model) {
        LocalDateTime reportDate = date != null ? LocalDateTime.parse(date) : LocalDateTime.now();
        model.addAttribute("bar", barService.getBarById(barId));
        model.addAttribute("report", reportService.getDailySalesReport(barId, reportDate));
        return "reports/daily";
    }

    // ================= REST INITIALIZE =================

    @PostMapping("/initialize")
    public ResponseEntity<InventorySessionDTO> initializeSession(
            @RequestParam Long barId,
            @RequestParam String shiftType,
            @RequestParam String notes) {

        InventorySession inv = sessionService.initializeSession(barId, shiftType, notes);

        InventorySessionDTO dto = InventorySessionDTO.builder()
                .sessionId(inv.getSessionId())
                .barId(inv.getBar().getBarId())
                .barName(inv.getBar().getBarName())
                .sessionStartTime(inv.getSessionStartTime())
                .status(inv.getStatus())
                .shiftType(inv.getShiftType())
                .notes(inv.getNotes())
                .build();

        return ResponseEntity.ok(dto);
    }

    // ================= HELPER =================

    private BigDecimal parseDecimal(String value) {
        return (value != null && !value.isEmpty()) ? new BigDecimal(value) : BigDecimal.ZERO;
    }
}