package life.qbic.portal.qoffer2.services

import life.qbic.portal.qoffer2.events.EventEmitter
import life.qbic.portal.qoffer2.offers.OfferDbConnector
import life.qbic.portal.qoffer2.shared.OfferOverview

/**
 * Service that contains basic overview data about available offers.
 *
 * This service offers an EventEmitter property that can be
 * used for inter component communication, when a new offer overview
 * source is available for download.
 *
 * @since 1.0.0
 */
class OverviewService implements Service {

    private List<OfferOverview> offerOverviewList

    private final OfferDbConnector offerDbConnector

    final EventEmitter<OfferOverview> updatedOverviewEvent

    OverviewService(OfferDbConnector offerDbConnector) {
        this.offerDbConnector = offerDbConnector
        this.updatedOverviewEvent = new EventEmitter<>()
        this.offerOverviewList = []
        reloadResources()
    }

    @Override
    void reloadResources() {
        offerOverviewList = offerDbConnector.loadOfferOverview()
    }

    /**
     * Returns a list of available offer overviews.
     * @return A list of available offer overviews.
     */
    List<OfferOverview> getOfferOverviewList() {
        final def overview = []
        /*
        We do not want to return a reference to the
        internal list, as this would make the list
        vulnerable for external changes.
        The list however contains immutable objects, these
        can be passed as reference.
         */
        overview.addAll(offerOverviewList.asList())
        return overview
    }
}
