package com.invoiceflow.ai.repository;

import com.invoiceflow.ai.entity.Invoice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findAllByUserId(UUID userId);

    Optional<Invoice> findByIdAndUserId(UUID invoiceId, UUID userId);

    boolean existsByUserIdAndInvoiceNumber(UUID userId, String invoiceNumber);
}
