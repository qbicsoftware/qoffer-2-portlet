package life.qbic.business.projects.spaces

import life.qbic.business.exceptions.ApplicationDeniedException
import life.qbic.business.exceptions.DatabaseQueryException

/**
 * Provides access to the project management datasource
 *
 * This interface collects methods to interact with the project management
 * datasource in the context of the Create Project Space use case.
 *
 * @since 1.0.0
 */
interface CreateProjectSpaceDataSource {

    /**
     * Creates a new space with the given name in QBiC's data management system
     *
     * @param spaceName The name of the space that should be created
     * @since 1.0.0
     * @throws ApplicationDeniedException If the project space name already exists
     * @throws DatabaseQueryException If a technical issue occurs during the data source interaction
     */
    void createProjectSpace(ProjectSpace projectSpace) throws ApplicationDeniedException,
            DatabaseQueryException

}
