package life.qbic.portal.offermanager.offergeneration

import org.jsoup.nodes.Document
import life.qbic.datamodel.dtos.business.*

/**
 * <h1>Describes the content of an offer HTML</h1>
 *
 * <p>An offer consists of a {@link QuotationOverview} and {@link QuotationDetails}. This class
 * generates the HTML code for each part of the offer.</p>
 *
 * <p>This class obtains the single html snippets from e.g the QuotationDetails class. Its only task is to manage the page content.
 * It iteratively adds items to a page and inserts a page break if an item would no longer fit onto a page. The element size of an item or an header,..
 * is calculated for that.</p>
 *
 * @since 1.1.0
 *
*/
class OfferHTMLDocument {


    private final Document htmlContent

    private final Offer offer

    QuotationOverview quotationOverview

    QuotationDetails quotationDetails

    OfferHTMLDocument(Document templateHTML, Offer offer){
        this.htmlContent = Objects.requireNonNull(templateHTML, "htmlContent object must not be a null reference")
        this.offer = Objects.requireNonNull(offer, "Offer object must not be a null reference")
        fillTemplateWithOfferContent()
    }

    /**
     * Fills the html template document with the offers content and returns
     * the final offer html
     * @param offer The offer object containing the required offer information
     * @return the final offer html
     */
    Document fillTemplateWithOfferContent(){

        quotationOverview = new QuotationOverview(htmlContent, offer)
        quotationDetails = new QuotationDetails(htmlContent, offer.items)
        return htmlContent
    }


}