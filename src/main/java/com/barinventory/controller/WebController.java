package com.barinventory.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.barinventory.entity.*;
import com.barinventory.service.*;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final BarService barService;
    private final ProductService productService;
    private final PricingService pricingService;
    private final InventorySessionService sessionService;
    private final ReportService reportService;

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
        return "/form";
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
                        ? new BigDecimal(volumeML)
                        : null)
                .unit(unit)
                .active(true)
                .build();

        productService.createProduct(product);
        return "redirect:/list";
    }

    // ================= PRICING =================

    @GetMapping("/pricing/{barId}")
    public String managePricing(@PathVariable Long barId, Model model) {

        Bar bar = barService.getBarById(barId);

        model.addAttribute("bar", bar);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("prices", pricingService.getPricesByBar(barId));

        return "manage";
    }

    @PostMapping("/pricing/{barId}/{productId}")
    public String savePrice(@PathVariable Long barId,
                            @PathVariable Long productId,
                            @RequestParam String sellingPrice,
                            @RequestParam(required = false) String costPrice) {

        BarProductPrice price = BarProductPrice.builder()
                .sellingPrice(new BigDecimal(sellingPrice))
                .costPrice(costPrice != null && !costPrice.isEmpty()
                        ? new BigDecimal(costPrice)
                        : BigDecimal.ZERO)
                .active(true)
                .build();

        pricingService.setPrice(barId, productId, price);

        return "/" + barId;
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

        InventorySession session =
                sessionService.initializeSession(barId, shiftType, notes);

        return "redirect:/stockroom/" + session.getSessionId();
    }

    // ================= STOCKROOM =================

    @GetMapping("/stockroom/{sessionId}")
    public String viewStockroom(@PathVariable Long sessionId, Model model) {

        InventorySession session = sessionService
                .getSessionById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        model.addAttribute("session", session);

        return "stockroom";
    }


    @PostMapping("/stockroom/{sessionId}")
    public String saveStockroom(@PathVariable Long sessionId,
                                @RequestParam Map<String, String> formData,
                                Model model) {

        try {

            InventorySession session = sessionService.getSession(sessionId);
            List<Product> products = productService.getAllActiveProducts();

            List<StockroomInventory> inventories = new ArrayList<>();

            for (Product product : products) {

                String opening = formData.get("opening_" + product.getProductId());
                String received = formData.get("received_" + product.getProductId());
                String closing = formData.get("closing_" + product.getProductId());
                String remarks = formData.get("remarks_" + product.getProductId());

                if (opening != null || received != null || closing != null) {

                    StockroomInventory inventory = StockroomInventory.builder()
                            .session(session)
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
            model.addAttribute("error", e.getMessage());
            return "stockroom";
        }
    }

    // ================= DISTRIBUTION =================

    @GetMapping("/sessions/distribution/{sessionId}")
    public String distributionPage(@PathVariable Long sessionId, Model model) {

        InventorySession session = sessionService.getSession(sessionId);

        model.addAttribute("session", session);
        model.addAttribute("bar", session.getBar());
        model.addAttribute("distributions", session.getDistributionRecords());

        return "distribution";
    }

    // ================= WELLS =================

    @GetMapping("/sessions/wells/{sessionId}")
    public String wellsPage(@PathVariable Long sessionId, Model model) {

        InventorySession session = sessionService.getSession(sessionId);

        if (session == null || session.getBar() == null) {
            throw new RuntimeException("Invalid session");
        }

        model.addAttribute("session", session);
        model.addAttribute("bar", session.getBar());
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("wellNames",
                Arrays.asList("BAR_1", "BAR_2", "SERVICE_BAR"));

        return "wells";
    }

    // ================= REPORTS =================

    @GetMapping("/reports/{barId}/daily")
    public String dailyReport(@PathVariable Long barId,
                              @RequestParam(required = false) String date,
                              Model model) {

        LocalDateTime reportDate =
                date != null ? LocalDateTime.parse(date) : LocalDateTime.now();

        model.addAttribute("bar", barService.getBarById(barId));
        model.addAttribute("report",
                reportService.getDailySalesReport(barId, reportDate));

        return "reports/daily";
    }

    // ================= REST INITIALIZE =================

    @PostMapping("/initialize")
    public ResponseEntity<InventorySessionDTO> initializeSession(
            @RequestParam Long barId,
            @RequestParam String shiftType,
            @RequestParam String notes) {

        InventorySession session =
                sessionService.initializeSession(barId, shiftType, notes);

        InventorySessionDTO dto = InventorySessionDTO.builder()
                .sessionId(session.getSessionId())
                .barId(session.getBar().getBarId())
                .barName(session.getBar().getBarName())
                .sessionStartTime(session.getSessionStartTime())
                .status(session.getStatus())
                .shiftType(session.getShiftType())
                .notes(session.getNotes())
                .build();

        return ResponseEntity.ok(dto);
    }

    // ================= HELPER =================

    private BigDecimal parseDecimal(String value) {
        return (value != null && !value.isEmpty())
                ? new BigDecimal(value)
                : BigDecimal.ZERO;
    }
}
