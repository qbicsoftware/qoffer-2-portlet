package life.qbic.portal.qoffer2.products

import groovy.sql.GroovyRowResult
import groovy.util.logging.Log4j2
import life.qbic.datamodel.dtos.business.ProductItem
import life.qbic.datamodel.dtos.business.services.*
import life.qbic.business.exceptions.DatabaseQueryException

import life.qbic.portal.qoffer2.database.ConnectionProvider
import org.apache.groovy.sql.extensions.SqlExtensions

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * This MariaDb connector offers access to available products.
 *
 * @since 1.0.0
 */
@Log4j2
class ProductsDbConnector {

  private final ConnectionProvider provider

  /**
   * Creates a connector for a MariaDB instance.
   *
   * The class instantiation will fail, if the passed provider is null.
   *
   * @param provider A connection provider
   */
  ProductsDbConnector(ConnectionProvider provider) {
    this.provider = Objects.requireNonNull(provider, "Provider must not be null.")
  }

  /**
   * Queries a data source for all available service
   * product that have been defined by the organisation.
   *
   * Throws a {@link DatabaseQueryException} if the query
   * fails for some reason. An exception must NOT be thrown,
   * if no product can be found. The returned list needs to
   * be empty then.
   *
   * @return A list of service {@link Product}.
   * @throws DatabaseQueryException
   */
  List<Product> findAllAvailableProducts() throws DatabaseQueryException {
    try {
      return fetchAllProductsFromDb()
    } catch (SQLException e) {
      log.error(e.message)
      log.error(e.stackTrace.join("\n"))
      throw new DatabaseQueryException("Unable to list all available products.")
    }
  }

  private List<Product> fetchAllProductsFromDb() {
    List<Product> products = []
    provider.connect().withCloseable {
      final PreparedStatement query = it.prepareStatement(Queries.SELECT_ALL_PRODUCTS)
      final ResultSet resultSet = query.executeQuery()
      products.addAll(convertResultSet(resultSet))
    }
    return products
  }

  private static List<Product> convertResultSet(ResultSet resultSet) {
    final def products = []
    while (resultSet.next()) {
      products.add(rowResultToProduct(SqlExtensions.toRowResult(resultSet)))
    }
    return products
  }

  private static Product rowResultToProduct(GroovyRowResult row) {
    def productCategory = row.category
    Product product
    switch(productCategory) {
      case "Data Storage":
        product = new DataStorage(row.productName as String,
            row.description as String,
            row.unitPrice as Double,
            new ProductUnitFactory().getForString(row.unit as String))
        break
      case "Primary Bioinformatics":
        product = new PrimaryAnalysis(row.productName as String,
            row.description as String,
            row.unitPrice as Double,
            new ProductUnitFactory().getForString(row.unit as String))
        break
      case "Project Management":
        product = new ProjectManagement(row.productName as String,
            row.description as String,
            row.unitPrice as Double,
            new ProductUnitFactory().getForString(row.unit as String))
        break
      case "Secondary Bioinformatics":
        product = new SecondaryAnalysis(row.productName as String,
            row.description as String,
            row.unitPrice as Double,
            new ProductUnitFactory().getForString(row.unit as String))
        break
      case "Sequencing":
        product = new Sequencing(row.productName as String,
            row.description as String,
            row.unitPrice as Double,
            new ProductUnitFactory().getForString(row.unit as String))
        break
    }
    if(product == null) {
      log.error("Product could not be parsed from database query.")
      log.error(row)
      throw new DatabaseQueryException("Cannot parse product")
    } else {
      return product
    }
  }

  def createOfferItems(List<ProductItem> items, int offerId) {

    items.each {productItem ->
      String query = "INSERT INTO productitem (productId, quantity, offerid) "+
              "VALUE(?,?,?)"

      int productId = findProductId(productItem.product)

      provider.connect().withCloseable {
        PreparedStatement preparedStatement = it.prepareStatement(query)
        preparedStatement.setInt(1,productId)
        preparedStatement.setDouble(2,productItem.quantity)
        preparedStatement.setInt(3,offerId)

        preparedStatement.execute()
      }
    }
  }

  /**
   * Searches for the product ID in the database
   *
   * @param connection through which the query is executed
   * @param product for which the ID needs to be found
   * @return the found ID
   */
  int findProductId(Product product) {
    String query = "SELECT id FROM product "+
            "WHERE category = ? AND description = ? AND productName = ? AND unitPrice = ? AND unit = ?"

    List<Integer> foundId = []

    provider.connect().withCloseable {
      PreparedStatement preparedStatement = it.prepareStatement(query)
      preparedStatement.setString(1, getProductType(product))
      preparedStatement.setString(2,product.description)
      preparedStatement.setString(3,product.productName)
      preparedStatement.setDouble(4,product.unitPrice)
      preparedStatement.setString(5,product.unit.value)
      ResultSet result = preparedStatement.executeQuery()

      while (result.next()){
        foundId << result.getInt(1)
      }
    }
    return foundId[0]
  }



  /**
   * Returns the product type of a product based on its implemented class
   *
   * @param product A product for which the type needs to be determined
   * @return the type of the product or null
   */
  static String getProductType(Product product){
    if (product instanceof Sequencing) return 'Sequencing'
    if (product instanceof ProjectManagement) return 'Project Management'
    if (product instanceof PrimaryAnalysis) return 'Primary Bioinformatics'
    if (product instanceof SecondaryAnalysis) return 'Secondary Bioinformatics'
    if (product instanceof DataStorage) return 'Data Storage'

    return null
  }

  /**
   * Queries all items of an offer.
   * @param offerPrimaryId The offer's primary key.
   * @return A list of offer-associated product items.
   */
  List<ProductItem> getItemsForOffer(int offerPrimaryId) {
    List<ProductItem> productItems = []
    Connection connection = provider.connect()
    connection.withCloseable {
      PreparedStatement statement = it.prepareStatement(Queries.SELECT_ALL_ITEMS_FOR_OFFER)
      statement.setInt(1, offerPrimaryId)
      ResultSet result = statement.executeQuery()
      while (result.next()) {
        Product product = rowResultToProduct(SqlExtensions.toRowResult(result))
        double quantity = result.getDouble("quantity")
        ProductItem item = new ProductItem(quantity, product)
        productItems << item
      }
    }
    return productItems
  }

  /**
   * Class that encapsulates the available SQL queries.
   */
  private static class Queries {

    /**
     * Query for all available products.
     */
    final static String SELECT_ALL_PRODUCTS = "SELECT * FROM product"

    /**
     * Query for all items of an offer.
     */
    final static String SELECT_ALL_ITEMS_FOR_OFFER =
            "SELECT * FROM productitem " +
                    "LEFT JOIN product ON productitem.productId = product.id " +
                    "WHERE offerId=?;"

  }
}
