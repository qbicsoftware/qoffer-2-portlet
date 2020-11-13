package life.qbic.portal.qoffer2.web.viewmodel

import com.vaadin.ui.Button
import groovy.beans.Bindable
import life.qbic.datamodel.accounting.ProductItem
import life.qbic.datamodel.dtos.business.Affiliation
import life.qbic.datamodel.dtos.business.Customer
import life.qbic.datamodel.dtos.business.ProjectManager

/**
 * A ViewModel holding data that is presented in a
 * life.qbic.portal.qoffer2.web.viewmodel.CreateOfferViewModel
 *
 * This class holds all specific fields that are mutable in the view
 * Whenever values change it should be reflected in the corresponding view. This class can be used
 * for UI unit testing purposes.
 *
 * This class can contain JavaBean objects to enable views to listen to changes in the values.
 *
 * @since: 0.1.0
 *
 */
class CreateOfferViewModel {

    @Bindable String projectTitle
    @Bindable String projectDescription
    @Bindable Customer customer
    @Bindable Affiliation customerAffiliation
    @Bindable ProjectManager projectManager
    @Bindable List<ProductItem> productItems
    @Bindable double totalPrice
    @Bindable Button next
    @Bindable Button back


}
