package life.qbic.business.persons.affiliation
/**
 * Lists all countries based on the iso standard
 *
 * @since: 1.0.0
 *
 */
class Country {

    /**
     * Provides a list of US English names of all known countries. This list contains unique entries and no empty Strings.
     * @return A list of unique country names in alphabetical order
     * @since 1.0.0
     */
    static List<String> availableCountryNames(){
        List<String> countryNames = Locale.US.getAvailableLocales().collect {it.getDisplayCountry()}.unique()
        countryNames.removeAll{it.isEmpty()}
        countryNames.sort()
        return countryNames
    }
}
