package life.qbic.portal.offermanager.components.offer.overview

import com.vaadin.data.provider.DataProvider
import com.vaadin.data.provider.ListDataProvider
import com.vaadin.icons.VaadinIcons
import com.vaadin.server.FileDownloader
import com.vaadin.server.StreamResource
import com.vaadin.shared.data.sort.SortDirection
import com.vaadin.shared.ui.grid.HeightMode
import com.vaadin.ui.Button
import com.vaadin.ui.FormLayout
import com.vaadin.ui.Grid
import com.vaadin.ui.HorizontalLayout
import com.vaadin.ui.Label
import com.vaadin.ui.ProgressBar
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.components.grid.HeaderRow
import com.vaadin.ui.themes.ValoTheme
import groovy.util.logging.Log4j2
import life.qbic.portal.offermanager.dataresources.offers.OfferUpdateService
import life.qbic.portal.offermanager.dataresources.offers.OfferOverview
import life.qbic.business.offers.Currency
import life.qbic.portal.offermanager.components.GridUtils

/**
 * A basic offer overview user interface.
 *
 * It presents basic available offer information, from which
 * the user can select and download one in PDF.
 *
 * @since 1.0.0
 */
@Log4j2
class OfferOverviewView extends FormLayout {

    final private OfferOverviewModel model

    final private Grid<OfferOverview> overviewGrid

    final private Button downloadBtn

    final Button updateOfferBtn

    final private ProgressBar downloadSpinner

    private FileDownloader fileDownloader

    final private OfferUpdateService offerUpdateService

    OfferOverviewView(OfferOverviewModel model, OfferUpdateService offerUpdateService) {
        this.model = model
        this.overviewGrid = new Grid<>()
        this.downloadBtn = new Button(VaadinIcons.DOWNLOAD)
        this.updateOfferBtn = new Button(VaadinIcons.EDIT)
        this.downloadSpinner = new ProgressBar()
        this.offerUpdateService = offerUpdateService

        initLayout()
        setupGrid()
        setupListeners()
    }

    private void initLayout() {
        /*
        We start with the header, that contains a descriptive
        title of what the view is about.
         */
        final VerticalLayout headerRow = new VerticalLayout()
        final Label label = new Label("Available Offers")

        label.addStyleName(ValoTheme.LABEL_HUGE)
        headerRow.addComponent(label)
        headerRow.setMargin(false)
        this.addComponent(headerRow)

        /*
        Below the header, we create content row with two components.
        The left component will be the offer overview, the
        right component will be the offer download button.
         */
        final HorizontalLayout activityContainer = new HorizontalLayout()
        downloadBtn.setStyleName(ValoTheme.BUTTON_LARGE)
        downloadBtn.setEnabled(false)
        downloadBtn.setDescription("Download offer")
        updateOfferBtn.setStyleName(ValoTheme.BUTTON_LARGE)
        updateOfferBtn.setEnabled(false)
        updateOfferBtn.setDescription("Update offer")
        // Makes the progress bar a spinner
        downloadSpinner.setIndeterminate(true)
        downloadSpinner.setVisible(false)
        activityContainer.addComponents(downloadBtn, updateOfferBtn, downloadSpinner)

        activityContainer.setMargin(false)
        headerRow.addComponents(activityContainer,overviewGrid)
        headerRow.setSizeFull()

        this.setMargin(false)
        this.setSpacing(false)

        this.setWidthFull()
    }

    private DataProvider setupDataProvider() {
        def dataProvider = new ListDataProvider(model.offerOverviewList)
        overviewGrid.setDataProvider(dataProvider)
        return dataProvider
    }

    private void setupGrid() {
        def dateColumn = overviewGrid.addColumn({ overview -> overview.getModificationDate() })
                .setCaption("Creation Date").setId("CreationDate")
        overviewGrid.addColumn({overview -> overview.offerId.toString()})
                .setCaption("Offer ID").setId("OfferId")
        overviewGrid.addColumn({overview -> overview.getProjectTitle()})
                .setCaption("Project Title").setId("ProjectTitle")
        overviewGrid.addColumn({overview -> overview.getCustomer()})
                .setCaption("Customer").setId("Customer")
        // fix formatting of price
        overviewGrid.addColumn({overview -> Currency.getFormatterWithSymbol().format(overview.getTotalPrice())}).setCaption("Total Price")
        overviewGrid.sort(dateColumn, SortDirection.DESCENDING)
        overviewGrid.setWidthFull()

        def offerOverviewDataProvider = setupDataProvider()

        setupFilters(offerOverviewDataProvider)
    }

    private void setupFilters(ListDataProvider<OfferOverview> offerOverviewDataProvider) {
        HeaderRow customerFilterRow = overviewGrid.appendHeaderRow()
        GridUtils.setupColumnFilter(offerOverviewDataProvider,
                overviewGrid.getColumn("OfferId"),
                customerFilterRow)
        GridUtils.setupColumnFilter(offerOverviewDataProvider,
                overviewGrid.getColumn("ProjectTitle"),
                customerFilterRow)
        GridUtils.setupColumnFilter(offerOverviewDataProvider,
                overviewGrid.getColumn("Customer"),
                customerFilterRow)
    }

    private void setupListeners() {
        overviewGrid.addSelectionListener(
                { selection ->
                    selection.firstSelectedItem.ifPresentOrElse({overview ->
                        UI.getCurrent().setPollInterval(50)
                        downloadSpinner.setVisible(true)
                        new LoadOfferInfoThread(UI.getCurrent(), overview).start()
                    }, {})
                })
        updateOfferBtn.addClickListener({
            offerUpdateService.addToResource(model.getSelectedOffer())
        })
    }

    private void createResourceForDownload() {
        removeExistingResources()
        StreamResource offerResource =
                new StreamResource((StreamResource.StreamSource res) -> {
                    return model.getOfferAsPdf()
                }, "${model.selectedOffer.identifier.toString()}.pdf")
        fileDownloader = new FileDownloader(offerResource)
        fileDownloader.extend(downloadBtn)
    }

    private void removeExistingResources() {
        if (fileDownloader) {
            downloadBtn.removeExtension(fileDownloader)
        }
    }

    private class LoadOfferInfoThread extends Thread {

        final private OfferOverview offerOverview

        final private UI ui

        LoadOfferInfoThread(UI ui, OfferOverview offerOverview) {
            this.ui = ui
            this.offerOverview = offerOverview
        }

        @Override
        void run() {
            ui.access(() -> {
                downloadSpinner.setVisible(true)
                overviewGrid.setEnabled(false)
                downloadBtn.setEnabled(false)
                updateOfferBtn.setEnabled(false)
            })

            model.setSelectedOffer(offerOverview)
            createResourceForDownload()

            ui.access(() -> {
                downloadSpinner.setVisible(false)
                overviewGrid.setEnabled(true)
                downloadBtn.setEnabled(true)
                updateOfferBtn.setEnabled(true)
                ui.setPollInterval(-1)
            })
        }
    }
}