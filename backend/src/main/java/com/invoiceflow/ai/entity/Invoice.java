package com.invoiceflow.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invoices_user_id_invoice_number",
                columnNames = {"user_id", "invoice_number"}
        )
)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotBlank
    @Size(max = 100)
    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @NotBlank
    @Size(max = 150)
    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @NotNull
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceDirection direction;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, length = 3)
    private String currency;

    @Size(max = 2_000)
    @Column(length = 2_000)
    private String notes;

    @Size(max = 2_048)
    @Column(name = "file_url", length = 2_048)
    private String fileUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Invoice() {
    }

    public Invoice(
            User user,
            Category category,
            String invoiceNumber,
            String vendorName,
            LocalDate invoiceDate,
            LocalDate dueDate,
            InvoiceStatus status,
            InvoiceDirection direction,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String currency,
            String notes,
            String fileUrl
    ) {
        this.user = user;
        this.category = category;
        this.invoiceNumber = invoiceNumber;
        this.vendorName = vendorName;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.status = status;
        this.direction = direction;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.notes = notes;
        this.fileUrl = fileUrl;
    }

    public Invoice(User user, Category category, String invoiceNumber, String vendorName, LocalDate invoiceDate,
                   LocalDate dueDate, InvoiceStatus status, BigDecimal subtotal, BigDecimal taxAmount,
                   BigDecimal totalAmount, String currency, String notes, String fileUrl) {
        this(user, category, invoiceNumber, vendorName, invoiceDate, dueDate, status, InvoiceDirection.EXPENSE,
                subtotal, taxAmount, totalAmount, currency, notes, fileUrl);
    }

    public void updateDetails(Category category, String invoiceNumber, String vendorName, LocalDate invoiceDate,
                              LocalDate dueDate, InvoiceStatus status, BigDecimal subtotal, BigDecimal taxAmount,
                              BigDecimal totalAmount, String currency, String notes, String fileUrl,
                              InvoiceDirection direction) {
        this.category = category;
        this.invoiceNumber = invoiceNumber;
        this.vendorName = vendorName;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.status = status;
        this.direction = direction;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.notes = notes;
        this.fileUrl = fileUrl;
    }

    public void updateStatus(InvoiceStatus status) {
        this.status = status;
    }

    public void updateFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void updateFromExtraction(String invoiceNumber, String vendorName, LocalDate invoiceDate,
                                      LocalDate dueDate, String currency, BigDecimal subtotal,
                                      BigDecimal taxAmount, BigDecimal totalAmount) {
        this.invoiceNumber = invoiceNumber;
        this.vendorName = vendorName;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.currency = currency;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Category getCategory() {
        return category;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getVendorName() {
        return vendorName;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public InvoiceDirection getDirection() {
        return direction;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNotes() {
        return notes;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
