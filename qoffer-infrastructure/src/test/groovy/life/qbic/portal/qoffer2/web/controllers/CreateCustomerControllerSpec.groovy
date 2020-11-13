package life.qbic.portal.qoffer2.web.controllers

import life.qbic.datamodel.dtos.business.AcademicTitle
import life.qbic.datamodel.dtos.business.AcademicTitleFactory
import life.qbic.datamodel.dtos.business.Affiliation
import life.qbic.datamodel.dtos.business.Customer
import life.qbic.portal.portlet.customers.create.CreateCustomerInput
import spock.lang.Specification

/**
 * Specification for the CreateCustomer Controller adapter
 *
 * These tests try to ensure that the conversion from view output to use case input is correct
 *
 * @since: 1.0.0
 */
class CreateCustomerControllerSpec extends Specification {


    def "CreateNewCustomer passes valid customer to use case"() {
        given:
        AcademicTitleFactory academicTitleFactory = new AcademicTitleFactory()
        AcademicTitle academicTitle = title ? academicTitleFactory.getForString(title) : AcademicTitle.NONE

        Affiliation affiliation = new Affiliation.Builder("Aperture", "Destructive way", "007", "Underground").build()
        List<Affiliation> affiliations = [ affiliation ]

        CreateCustomerInput createCustomerInput = Mock()
        CreateCustomerController controller = new CreateCustomerController(createCustomerInput)
        when:
        controller.createNewCustomer(firstName, lastName, title, email, affiliations)
        then:
        1 * createCustomerInput.createCustomer({Customer customer ->
            customer.firstName == firstName && \
            customer.lastName == lastName && \
            customer.title == academicTitle && \
            customer.emailAddress == email && \
            customer.affiliations == affiliations
        })
        where:
        firstName |lastName |title |email
        "John" | "Doe" | null | "john@doe.com"
        "John" | "Doe" | "Dr." | "john@doe.com"
        "John" | "Doe" | "Prof. Dr." | "john@doe.com"
    }
}
