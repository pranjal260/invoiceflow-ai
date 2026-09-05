package com.invoiceflow.ai.repository;

import com.invoiceflow.ai.entity.InvoiceItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
    List<InvoiceItem> findAllByInvoiceId(UUID invoiceId);

    Optional<InvoiceItem> findByIdAndInvoiceId(UUID invoiceItemId, UUID invoiceId);

    void deleteAllByInvoiceId(UUID invoiceId);
}
