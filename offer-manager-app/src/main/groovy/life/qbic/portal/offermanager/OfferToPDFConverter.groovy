package life.qbic.portal.offermanager

import groovy.util.logging.Log4j2
import life.qbic.datamodel.dtos.business.AcademicTitle
import life.qbic.datamodel.dtos.business.Affiliation
import life.qbic.datamodel.dtos.business.Customer
import life.qbic.datamodel.dtos.business.Offer
import life.qbic.datamodel.dtos.business.ProductItem
import life.qbic.datamodel.dtos.business.ProjectManager
import life.qbic.business.offers.Currency
import life.qbic.business.offers.OfferExporter
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.DateFormat

/**
 * Handles the conversion of offers to pdf files
 *
 * Implements {@link OfferExporter} and is responsible for converting an offer into PDF Format
 *
 * @since: 1.0.0
 * @author: Jennifer Bödker
 *
 */
@Log4j2
class OfferToPDFConverter implements OfferExporter {

    /**
     * The environment variable that is resolved when
     * the chromium browser needs to be executed
     * for the PDF conversion.
     *
     * This variable's value must contain the chromium browser
     * alias that can be executed from the system's command
     * line.
     */
    static final CHROMIUM_EXECUTABLE = "CHROMIUM_EXECUTABLE"

    /**
     * Stores the label used in the html document for sections associated with overhead cost
     * If the String value is changed here it also has to be adapted in the html document
     */
    static final OVERHEAD = "overhead"

    /**
     * Stores the label used in the html document for sections associated without an overhead cost
     * If the String value is changed here, it also has to be adapted in the html document
     */
    static final NO_OVERHEAD = "no-overhead"

    /**
     * Number of maximal items per table
     */
    private final int maxTableItems = 8


    /**
     * Variable used to count the number of generated productTables in the Offer PDF
     */
    private static int tableCount

    private final Offer offer

    private final Path tempDir

    private final Document htmlContent

    private final Path createdOffer

    private final Path createdOfferPdf

    private final Path newOfferImage

    private final Path newOfferStyle

    private static final Path OFFER_HTML_TEMPLATE =
            Paths.get(OfferToPDFConverter.class.getClassLoader()
                    .getResource("offer-template/offer.html")
                    .toURI())
    private static final Path OFFER_HEADER_IMAGE =
            Paths.get(OfferToPDFConverter.class.getClassLoader()
                    .getResource("offer-template/offer_header.png")
                    .toURI())
    private static final Path OFFER_STYLESHEET =
            Paths.get(OfferToPDFConverter.class.getClassLoader()
                    .getResource("offer-template/stylesheet.css")
                    .toURI())

    OfferToPDFConverter(Offer offer) {
        this.offer = Objects.requireNonNull(offer, "Offer object must not be a null reference")
        this.tempDir = Files.createTempDirectory("offer")
        this.createdOffer = Paths.get(tempDir.toString(), "offer.html")
        this.newOfferImage = Paths.get(tempDir.toString(), "offer_header.png")
        this.newOfferStyle = Paths.get(tempDir.toString(), "stylesheet.css")
        this.createdOfferPdf = Paths.get(tempDir.toString(), "offer.pdf")
        importTemplate()
        this.htmlContent = Parser.xmlParser().parseInput(new File(this.createdOffer.toUri()).text, "")
        fillTemplateWithOfferContent()
        writeHTMLContentToFile(this.createdOffer, this.htmlContent)
        generatePDF()
    }

    InputStream getOfferAsPdf() {
        return new BufferedInputStream(new FileInputStream(new File(createdOfferPdf.toString())))
    }

    private void importTemplate() {
        Files.copy(OFFER_HTML_TEMPLATE, createdOffer, StandardCopyOption.REPLACE_EXISTING)
        Files.copy(OFFER_HEADER_IMAGE, newOfferImage, StandardCopyOption.REPLACE_EXISTING)
        Files.copy(OFFER_STYLESHEET, newOfferStyle, StandardCopyOption.REPLACE_EXISTING)
    }

    private static void writeHTMLContentToFile(Path fileLocation, Document htmlContent) {
        new File(fileLocation.toUri()).withWriter {
            it.write(htmlContent.toString())
            it.flush()
        }
    }

    private void fillTemplateWithOfferContent() {
        setProjectInformation()
        setCustomerInformation()
        setManagerInformation()
        setSelectedItems()
        setTotalPrices()
        setQuotationDetails()
    }

    private void generatePDF() {
        PdfPrinter pdfPrinter = new PdfPrinter(createdOffer)
        pdfPrinter.print(createdOfferPdf)
    }

    private void setProjectInformation() {
        htmlContent.getElementById("project-title").text(offer.projectTitle)
        htmlContent.getElementById("project-description").text(offer.projectDescription)
    }

    private void setCustomerInformation() {
        final Customer customer = offer.customer
        final Affiliation affiliation = offer.selectedCustomerAffiliation
        String customerTitle = customer.title == AcademicTitle.NONE ? "" : customer.title
        htmlContent.getElementById("customer-name").text(String.format(
                "%s %s %s",
                customerTitle,
                customer.firstName,
                customer.lastName))
        htmlContent.getElementById("customer-organisation").text(affiliation.organisation)
        htmlContent.getElementById("customer-street").text(affiliation.street)
        htmlContent.getElementById("customer-postal-code").text(affiliation.postalCode)
        htmlContent.getElementById("customer-city").text(affiliation.city)
        htmlContent.getElementById("customer-country").text(affiliation.country)

    }

    private void setManagerInformation() {
        final ProjectManager pm = offer.projectManager
        final Affiliation affiliation = pm.affiliations.get(0)
        String pmTitle = pm.title == AcademicTitle.NONE ? "" : pm.title
        htmlContent.getElementById("project-manager-name").text(String.format(
                "%s %s %s",
                pmTitle,
                pm.firstName,
                pm.lastName))
        htmlContent.getElementById("project-manager-street").text(affiliation.street)
        htmlContent.getElementById("project-manager-city").text("${affiliation.postalCode} ${affiliation.city}")
        htmlContent.getElementById("project-manager-email").text(pm.emailAddress)
    }

    void setSelectedItems() {
        // Let's clear the existing item template content first
        htmlContent.getElementById("product-items-1").empty()
        //and remove the footer on the first page
        //htmlContent.getElementById("grid-table-footer").remove()

        List<ProductItem> listOverheadItems = offer.itemsWithOverhead
        List<ProductItem> listNoOverheadItems = offer.itemsWithoutOverhead

        //Initialize Number of table
        tableCount = 1

        //Initialize Count of ProductItems in table
        int tableItemsCount = 1
        //Generate ProductTable for Overhead and Non-Overhead Product Items
        generateProductTable(OVERHEAD, listOverheadItems, tableItemsCount)
        generateProductTable(NO_OVERHEAD, listNoOverheadItems, tableItemsCount)

        //Append total cost footer
        if (tableItemsCount >= maxTableItems) {
            //If currentTable is filled with Items generate new one and add total pricing there
            ++tableCount
            String elementId = "product-items" + "-" + tableCount
            htmlContent.getElementById("item-table-grid").append(ItemPrintout.tableHeader(elementId))
            htmlContent.getElementById("item-table-grid")
                    .append(ItemPrintout.tableFooter())
        } else {
            //otherwise add total pricing to table
            htmlContent.getElementById("item-table-grid")
                    .append(ItemPrintout.tableFooter())
        }
        }


    void setTotalPrices() {
        final totalPrice = Currency.getFormatterWithoutSymbol().format(offer.totalPrice)
        final taxes = Currency.getFormatterWithoutSymbol().format(offer.taxes)
        final netPrice = Currency.getFormatterWithoutSymbol().format(offer.netPrice)
        final netPrice_withSymbol = Currency.getFormatterWithSymbol().format(offer.netPrice)

        htmlContent.getElementById("total-costs-net").text(netPrice_withSymbol)
        htmlContent.getElementById("total-cost-value-net").text(netPrice)
        htmlContent.getElementById("vat-cost-value").text(taxes)
        htmlContent.getElementById("final-cost-value").text(totalPrice)
    }

    void setQuotationDetails() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.US)

        htmlContent.getElementById("offer-identifier").text(offer.identifier.toString())
        htmlContent.getElementById("offer-expiry-date").text(offer.expirationDate.toLocalDate().toString())
        htmlContent.getElementById("offer-date").text(dateFormat.format(offer.modificationDate))
    }

    void setIntermediatePrices(String overheadStatus) {

        double itemsNetPrice
        double overheadPrice
        double totalPrice

        if (overheadStatus == "overhead") {
            itemsNetPrice = offer.getItemsWithOverheadNetPrice()
            overheadPrice = offer.getOverheads()
            totalPrice = itemsNetPrice + overheadPrice
        }
        else {
            //No Overhead Prices are calculated for these items
            overheadPrice = 0.00
            itemsNetPrice = offer.getItemsWithOverheadNetPrice()
            totalPrice = itemsNetPrice
        }

            final String formattedTotalPrice = Currency.getFormatterWithoutSymbol().format(totalPrice)
            final String formattedItemsWithOverheadNetPrice = Currency.getFormatterWithoutSymbol().format(itemsNetPrice)
            final String formattedOverheadPrice = Currency.getFormatterWithoutSymbol().format(overheadPrice)

            htmlContent.getElementById("${overheadStatus}-value-total").text(formattedTotalPrice)
            htmlContent.getElementById("${overheadStatus}-value-net").text(formattedItemsWithOverheadNetPrice)
            htmlContent.getElementById("${overheadStatus}-value-overhead").text(formattedOverheadPrice)
        }

    void generateProductTable(String overheadStatus, List<ProductItem> productItems, int tableItemsCount){
        // Create the items in html in the overview table
        if (!productItems.isEmpty()) {
        // set initial product item position in table
        def itemPos = 1
        // max number of table items per page
        def elementId = "product-items" + "-" + tableCount
            //Append Table Title
            htmlContent.getElementById(elementId).append(ItemPrintout.tableTitle(overheadStatus))
            productItems.each { productItem ->
                //start (next) table and add Product to it
                if (tableItemsCount >= maxTableItems)
                {
                    ++tableCount
                    elementId = "product-items" + "-" + tableCount
                    htmlContent.getElementById("item-table-grid").append(ItemPrintout.tableHeader(elementId))
                    tableItemsCount = 1
                }
                //add product to current table
                htmlContent.getElementById(elementId).append(ItemPrintout.itemInHTML(itemPos++, productItem))
                tableItemsCount++
            }
            // Add subtotal Footer to ProductItem
            htmlContent.getElementById(elementId).append(ItemPrintout.subTotalFooter(overheadStatus))
            tableItemsCount++
            //Update Pricing of Footer
            setIntermediatePrices(overheadStatus)
        }
    }

    /**
     * Small helper class to handle the HTML to PDF conversion.
     */
    class PdfPrinter {

        final Path sourceFile

        final String chromeAlias

        PdfPrinter(Path sourceFile) {
            this.sourceFile = sourceFile
            this.chromeAlias = Objects.requireNonNull(System.getenv(CHROMIUM_EXECUTABLE),
                    "CHROMIUM_EXECUTABLE environment was not set.")
        }

        PdfPrinter(Path sourceFile, String alias) {
            this.sourceFile = sourceFile
            this.chromeAlias = alias
        }

        void print(Path outputFile) {
            final Path output = outputFile
            ProcessBuilder builder = new ProcessBuilder()
            builder.command(chromeAlias,
                    "--headless",
                    "--disable-gpu",
                    "--aggressive-cache-discard",
                    "--print-to-pdf-no-header",
                    "--print-to-pdf=${output.toString()}",
                    "${sourceFile}")
            builder.directory(new File(sourceFile.getParent().toString()))
            builder.redirectErrorStream(true)
            Process process = builder.start()
            process.waitFor()
            process.getInputStream().eachLine {log.info(it)}
            if (! new File(output.toString()).exists()) {
                throw new RuntimeException("Offer PDF has not been generated.")
            }
        }
    }

    private static class ItemPrintout {

        static String itemInHTML(int offerPosition, ProductItem item) {
            String totalCost = Currency.getFormatterWithoutSymbol().format(item.quantity * item.product.unitPrice)
            return """<div class="row product-item">
                        <div class="col-1">${offerPosition}</div>
                        <div class="col-4 ">${item.product.productName}</div>
                        <div class="col-1 price-value">${item.quantity}</div>
                        <div class="col-2 text-center">${item.product.unit}</div>
                        <div class="col-2 price-value">${Currency.getFormatterWithoutSymbol().format(item.product.unitPrice)}</div>
                        <div class="col-2 price-value">${totalCost}</div>
                    </div>
                    <div class="row product-item">
                        <div class="col-1"></div>
                        <div class="col-4 item-description">${item.product.description}</div>
                        <div class="col-7"></div>
                    </div>
                    """

        }

        static String subTotalFooter(String overheadStatus) {

            return """
            <div class="row total-costs double-underscore" id = "${overheadStatus}-total">
            <div class="col-6"></div>
                    <div class="col-4 cost-summary-field">Total Cost:</div>
            <div class="col-2 price-value" id="${overheadStatus}-value-total">4000</div>
                    </div>
            <div class="row total-costs" id = "${overheadStatus}-net">
            <div class="col-10 cost-summary-field">Net Cost</div>
                    <div class="col-2 price-value" id="${overheadStatus}-value-net">3000</div>
            </div>
                    <div class="row total-costs" id ="${overheadStatus}-overhead">
                    <div class="col-10 cost-summary-field">Overhead Cost:</div>
            <div class="col-2 price-value" id="${overheadStatus}-value-overhead">1000</div>
            </div>
            <div class ="small-spacer"> </div> 
            """
        }

        static String tableHeader(String elementId) {
            //1. add pagebreak
            //2. create empty table for elementId
            return """<div class="pagebreak"></div>
                                     <div class="row table-header" id="grid-table-header">
                                         <div class="col-1">Pos.</div>
                                         <div class="col-4">Service Description</div>
                                         <div class="col-1 price-value">Amount</div>
                                         <div class="col-2 text-center">Unit</div>
                                         <div class="col-2 price-value">Price/Unit (€)</div>
                                         <div class="col-2 price-value">Total (€)</div>
                                    </div>
                                 <div class="product-items" id="${elementId}"></div>
                             """
        }

        static String tableTitle(String overheadStatus){
            String tableTitle
            if (overheadStatus == "overhead"){
                tableTitle = "Products with Overhead"
            }
            else {
                tableTitle = "Products without Overheads"
            }
            return """<div class = "small-spacer"</div>
                    <h3>${tableTitle}</h3>
                   """
        }

        static String tableFooter(){
            return """<div id="grid-table-footer">
                                     <div class="row total-costs" id = "offer-net">
                                         <div class="col-6"></div>
                                         <div class="col-4 cost-summary-field">Estimated total (net):</div>
                                         <div class="col-2 price-value" id="total-cost-value-net">12,500.00</div>
                                     </div>
                                     <div class="row total-costs" id = "offer-vat">
                                         <div class="col-10 cost-summary-field">VAT (19%):</div>
                                         <div class="col-2 price-value" id="vat-cost-value">0.00</div>
                                     </div>
                                     <div class="row total-costs" id ="offer-total">
                                         <div class="col-10 cost-summary-field">Estimated total (VAT included):</div>
                                         <div class="col-2 price-value" id="final-cost-value">12,500.00</div>
                                     </div>
                                 </div>
                                 """
        }
        }
}
