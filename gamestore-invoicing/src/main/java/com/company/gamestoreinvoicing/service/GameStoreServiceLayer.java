package com.company.gamestoreinvoicing.service;


import com.company.gamestoreinvoicing.feign.GameStoreCatalog;
import com.company.gamestoreinvoicing.viewModel.ConsoleViewModel;
import com.company.gamestoreinvoicing.viewModel.GameViewModel;
import com.company.gamestoreinvoicing.viewModel.TShirtViewModel;
import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Component;

        import java.math.BigDecimal;
        import java.math.RoundingMode;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.Optional;
import com.company.gamestoreinvoicing.repository.*;
import com.company.gamestoreinvoicing.model.*;

        import com.company.gamestoreinvoicing.viewModel.InvoiceViewModel;


@Component
public class GameStoreServiceLayer {
    private final BigDecimal MAX_PROCESSING_FEE = new BigDecimal("15.49");
    private final BigDecimal INVOICE_TOTAL = new BigDecimal("999.99");
    private final String GAME_PRODUCT_TYPE = "Game";
    private final String CONSOLE_PRODUCT_TYPE = "Console";
    private final String TSHIRT_PRODUCT_TYPE = "T-Shirt";

    InvoiceRepository invoiceRepo;
    TaxRepository taxRepo;
    ProcessingFeeRepository processingFeeRepo;

    @Autowired
    private final GameStoreCatalog gameStoreCatalog;

    GameStoreServiceLayer(InvoiceRepository invoiceRepo, TaxRepository taxRepo, ProcessingFeeRepository processingFeeRepo, GameStoreCatalog gameStoreCatalog) {
        this.invoiceRepo = invoiceRepo;
        this.taxRepo = taxRepo;
        this.processingFeeRepo = processingFeeRepo;
        this.gameStoreCatalog = gameStoreCatalog;
    }


    public InvoiceViewModel createInvoice(InvoiceViewModel invoiceViewModel) {


        if (invoiceViewModel == null)
            throw new NullPointerException(" no  data.");

        if (invoiceViewModel.getProductType() == null)
            throw new IllegalArgumentException("no Item type. ");


        if (invoiceViewModel.getQuantity() <= 0) {
            throw new IllegalArgumentException(invoiceViewModel.getQuantity() +
                    "no quantity");
        }

        Invoice invoice = new Invoice();
        invoice.setName(invoiceViewModel.getName());
        invoice.setStreet(invoiceViewModel.getStreet());
        invoice.setCity(invoiceViewModel.getCity());
        invoice.setState(invoiceViewModel.getState());
        invoice.setZipcode(invoiceViewModel.getZipcode());
        invoice.setProductType(invoiceViewModel.getProductType());
        invoice.setItemId(invoiceViewModel.getItemId());
/*
(invoiceViewModel.getItemType().equals(CONSOLE_ITEM_TYPE)) {
            Console tempCon = null;
            Optional<Console> returnVal = consoleRepo.findById(invoiceViewModel.getItemId());
              Optional<Artist> artist = artistRepository.findById(album.getArtistId());
 */

        if (invoiceViewModel.getProductType().equals(CONSOLE_PRODUCT_TYPE)) {
            ConsoleViewModel tempCon = null;
           Optional<ConsoleViewModel> returnVal = Optional.ofNullable(gameStoreCatalog.getConsole(invoiceViewModel.getItemId()));
            //Optional<ConsoleViewModel> returnVal =InvoiceViewModel.getProductType(invoiceViewModel.setProductType());
            if (returnVal.isPresent()) {
                tempCon = returnVal.get();
            } else {
                throw new IllegalArgumentException("Requested item is unavailable.");
            }

            if (invoiceViewModel.getQuantity() > tempCon.getQuantity()) {
                throw new IllegalArgumentException("Requested quantity is unavailable.");
            }

            invoice.setUnitPrice(tempCon.getPrice());

        } else if (invoiceViewModel.getProductType().equals(GAME_PRODUCT_TYPE)) {
            GameViewModel tempGame = null;

            Optional<GameViewModel> returnVal = Optional.ofNullable(gameStoreCatalog.getGame(invoiceViewModel.getItemId()));

            if (returnVal.isPresent()) {
                tempGame = returnVal.get();
            } else {
                throw new IllegalArgumentException("Requested item is unavailable.");
            }

            if (invoiceViewModel.getQuantity() > tempGame.getQuantity()) {
                throw new IllegalArgumentException("Requested quantity is unavailable.");
            }
            invoice.setUnitPrice(tempGame.getPrice());


        } else if (invoiceViewModel.getProductType().equals(TSHIRT_PRODUCT_TYPE)) {
            TShirtViewModel tempTShirt = null;

            Optional<TShirtViewModel> returnVal = Optional.ofNullable(gameStoreCatalog.getTShirt(invoiceViewModel.getItemId()));

            if (returnVal.isPresent()) {
                tempTShirt = returnVal.get();
            } else {
                throw new IllegalArgumentException("Requested item is unavailable.");
            }

            if (invoiceViewModel.getQuantity() > tempTShirt.getQuantity()) {
                throw new IllegalArgumentException("Requested quantity is unavailable.");
            }
            invoice.setUnitPrice(tempTShirt.getPrice());

        } else {
            throw new IllegalArgumentException(invoiceViewModel.getProductType() +
                    ": Unrecognized Item type. Valid ones: T-Shirt, Console, or Game");
        }

        invoice.setQuantity(invoiceViewModel.getQuantity());
        invoice.setSubtotal(
                invoice.getUnitPrice().multiply(
                        new BigDecimal(invoiceViewModel.getQuantity())).setScale(2, RoundingMode.HALF_UP));

        if ((invoice.getSubtotal().compareTo(new BigDecimal(999.99)) > 0)) {
            throw new IllegalArgumentException("Subtotal  price of 999.99");
        }


        BigDecimal tempTaxRate;
        Optional<Tax> returnVal = taxRepo.findById(invoice.getState());

        if (returnVal.isPresent()) {
            tempTaxRate = returnVal.get().getRate();
        } else {
            throw new IllegalArgumentException(invoice.getState() + "Invalid State code.");
        }

        if (!tempTaxRate.equals(BigDecimal.ZERO)) {
            invoice.setTax(tempTaxRate.multiply(invoice.getSubtotal()));
        } else {
            throw new IllegalArgumentException(invoice.getState() + "Invalid State code.");
        }

        BigDecimal processingFee;
        Optional<ProcessingFee> returnVal2 = processingFeeRepo.findById(invoiceViewModel.getProductType());

        if (returnVal2.isPresent()) {
            processingFee = returnVal2.get().getFee();
        } else {
            throw new IllegalArgumentException("Requested item is unavailable.");
        }

        invoice.setProcessingFee(processingFee);


        if (invoiceViewModel.getQuantity() > 10) {

            invoice.setProcessingFee(invoice.getProcessingFee().add(invoice.getProcessingFee()));
        }

        invoice.setTotal(invoice.getSubtotal().add(invoice.getProcessingFee()).add(invoice.getTax()));

        if ((invoice.getTotal().compareTo(INVOICE_TOTAL) > 0)) {
            throw new IllegalArgumentException("Subtotal exceeds maximum purchase price of $999.99");
        }

        invoice = invoiceRepo.save(invoice);

        return buildInvoiceViewModel(invoice);
    }

    public InvoiceViewModel getInvoice(long id) {
        Optional<Invoice> invoice = invoiceRepo.findById(id);
        if (invoice == null)
            return null;
        else
            return buildInvoiceViewModel(invoice.get());
    }

    public List<InvoiceViewModel> getAllInvoices() {
        List<Invoice> invoiceList = invoiceRepo.findAll();
        List<InvoiceViewModel> ivmList = new ArrayList<>();
        List<InvoiceViewModel> exceptionList = null;

        if (invoiceList == null) {
            return exceptionList;
        } else {
            invoiceList.stream().forEach(i -> {
                ivmList.add(buildInvoiceViewModel(i));
            });
        }
        return ivmList;
    }

    public List<InvoiceViewModel> getInvoicesByCustomerName(String name) {
        List<Invoice> invoiceList = invoiceRepo.findByName(name);
        List<InvoiceViewModel> ivmList = new ArrayList<>();
        List<InvoiceViewModel> exceptionList = null;

        if (invoiceList == null) {
            return exceptionList;
        } else {
            invoiceList.stream().forEach(i -> ivmList.add(buildInvoiceViewModel(i)));
        }
        return ivmList;
    }

    public void deleteInvoice(long id) {
        invoiceRepo.deleteById(id);
    }

    public InvoiceViewModel buildInvoiceViewModel(Invoice invoice) {
        InvoiceViewModel invoiceViewModel = new InvoiceViewModel();
        invoiceViewModel.setId(invoice.getId());
        invoiceViewModel.setName(invoice.getName());
        invoiceViewModel.setStreet(invoice.getStreet());
        invoiceViewModel.setCity(invoice.getCity());
        invoiceViewModel.setState(invoice.getState());
        invoiceViewModel.setZipcode(invoice.getZipcode());
        invoiceViewModel.setProductType(invoice.getProductType());
        invoiceViewModel.setItemId((int) invoice.getItemId());
        invoiceViewModel.setUnitPrice(invoice.getUnitPrice());
        invoiceViewModel.setQuantity(invoice.getQuantity());
        invoiceViewModel.setSubtotal(invoice.getSubtotal());
        invoiceViewModel.setProcessingFee(invoice.getProcessingFee());
        invoiceViewModel.setTax(invoice.getTax());
        invoiceViewModel.setProcessingFee(invoice.getProcessingFee());
        invoiceViewModel.setTotal(invoice.getTotal());

        return invoiceViewModel;
    }

}


