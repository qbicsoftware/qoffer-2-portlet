package life.qbic.portal.qoffer2.customers

import life.qbic.datamodel.dtos.business.Customer
import life.qbic.datamodel.people.Person
import life.qbic.portal.portlet.CriteriaType

import life.qbic.portal.portlet.customers.CustomerDbGateway
import life.qbic.portal.portlet.SearchCriteria
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger


/**
 * Provides operations on QBiC customer data
 *
 * This class implements {@link CustomerDbGateway} and is responsible for transferring data from the database into qOffer
 *
 * @since: 1.0.0
 * @author: Jennifer Bödker
 *
 */
class CustomerDbConnector implements CustomerDbGateway{

  CustomerDatabaseQueries databaseQueries

  private static final Logger LOG = LogManager.getLogger(CustomerDbConnector.class)

  CustomerDbConnector(CustomerDatabaseQueries databaseQueries){
    this.databaseQueries = databaseQueries
  }

  @Override
  List<Customer> findCustomer(SearchCriteria criteria) {
    String searchCondition = criteria.criteriaValue

    switch(criteria.criteriaType){
      case CriteriaType.LAST_NAME:
        //todo create a Customer
        List<Person> person = databaseQueries.findPersonByName(searchCondition)
            return null
      case CriteriaType.GROUP_NAME:
            return databaseQueries.findCustomerByGroup(searchCondition)
      case CriteriaType.ADD_ADDRESS:
            return databaseQueries.findCustomerByAdditionalAddress(searchCondition)
      case CriteriaType.CITY:
            return databaseQueries.findCustomerByCity(searchCondition)
      default:
        //todo throw an exception
        return null
    }

  }

  @Override
  void addCustomer(Customer customer) {
    databaseQueries.addCustomer(customer)

  }

  @Override
  void updateCustomer(String customerId, Customer updatedCustomer) {
    databaseQueries.updateCustomer(customerId,updatedCustomer)

  }
}
