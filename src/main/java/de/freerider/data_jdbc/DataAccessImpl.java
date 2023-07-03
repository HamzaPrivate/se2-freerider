package de.freerider.data_jdbc;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freerider.datamodel.Customer;
import de.freerider.datamodel.DataFactory;
import de.freerider.datamodel.Reservation;


/**
 * Non-public implementation class or DataAccess interface.
 */
@Component
class DataAccessImpl implements DataAccess {

    /*
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(DataAccessImpl.class);

    /**
     * Datafactory is a component that creates datamodel objects.
     */
    @Autowired
    private DataFactory dataFactory;

    /*
     * JdbcTemplate is the central class in the JDBC core package for SQL
     * database access.
     * - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html
     * 
     * Examples:
     * - https://mkyong.com/spring-boot/spring-boot-jdbc-examples
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Run query that returns the number of Customers in the database:
     * - query: SELECT COUNT(ID) FROM CUSTOMER;
     * - returns number extracted from ResultSet.
     * 
     * @return number of Customer records in the database.
     */
    @Override
    public long countCustomers() {
        //
        List<Object> result = jdbcTemplate.query(
            /*
             * Run SQL statement:
             */
            "SELECT COUNT(ID) FROM CUSTOMER",

            /*
             * Return ResultSet (rs) and extract COUNT value.
             */
            (rs, rowNum) -> {
                long count = rs.getInt(1);  // index[1]
                return count;
            }
        );
        //
        return result.size() > 0? (long)(result.get(0)) : 0;
    }


    /**
     * Run query that returns all Customers in the database.
     * - query: SELECT * FROM CUSTOMER;
     * - returns Customer objects created from ResultSet rows.
     * 
     * @return all Customers in the database.
     */
    @Override
    public Iterable<Customer> findAllCustomers() {
        //
        var result = jdbcTemplate.queryForStream(
            /*
             * Run SQL statement:
             */
            "SELECT * FROM CUSTOMER",

            /*
             * Return ResultSet (rs) and map each row to Optional<Customer>
             * depending on whether the object could be created from values
             * returned from the database or not (empty Optional is returned).
             */
            (rs, rowNum) -> {
                /*
                 * Extract values from ResultSet for each row.
                 */
                long id = rs.getInt("ID");
                String name = rs.getString("NAME");
                String contact = rs.getString("CONTACT");
                String status = rs.getString("STATUS");
                /*
                 * Attempt to create Customer object through dataFactory,
                 * which returns Optional<Customer>.
                 */
                return dataFactory.createCustomer(id, name, contact, status);
            }
        )
        /*
         * Remove empty results from stream of Optional<Customer>,
         * map remaining from Optional<Customer> to Customer and
         * collect result.
         */
        .filter(opt -> opt.isPresent())
        .map(opt -> opt.get())
        .collect(Collectors.toList());
        //
        return result;
    }


    /**
     * Run query that returns one Customers with a given id.
     * - query: SELECT * FROM CUSTOMER WHERE ID = ?id;
     * - returns Customer object created from ResultSet row.
     * 
     * @param id Customer id (WHERE ID = id)
     * @return Optional with Customer or empty if not found.
     */
    @Override
    public Optional<Customer> findCustomerById(long id) {
        //
        List<Optional<Customer>> result = jdbcTemplate.query(
            /*
             * Prepare statement (ps) with "?"-augmented SQL query.
             */
            "SELECT * FROM CUSTOMER WHERE ID = ?",
            ps -> {
                /*
                 * Insert id value of first occurence of "?" in SQL.
                 */
                ps.setInt(1, (int)id);
            },

            (rs, rowNum) -> {
                /*
                 * Extract values from ResultSet.
                 */
                String name = rs.getString("NAME");
                String contact = rs.getString("CONTACT");
                String status = rs.getString("STATUS");
                /*
                 * Create Optional<Customer> from values.
                 */
                return dataFactory.createCustomer(id, name, contact, status);
            }
        );
        /*
         * Probe List<Optional<Customer>> and return Optional<Customer> or
         * empty Optional for empty list.
         */
        return result.size() > 0? result.get(0) : Optional.empty();
    }

    // public long numberOfReservationsByCustomer(long id) {
    //     //
    //     List<Object> result = jdbcTemplate.query(
    //         /*
    //          * Prepare statement (ps) with "?"-augmented SQL query.
    //          */
    //         "SELECT * FROM CUSTOMER WHERE ID = ?",
    //         ps -> {
    //             /*
    //              * Insert id value of first occurence of "?" in SQL.
    //              */
    //             ps.setInt(1, (int)id);
    //         },

    //         (rs, rowNum) -> {
    //             /*
    //              * Extract values from ResultSet.
    //              */
    //             String name = rs.getString("NAME");
    //             String contact = rs.getString("CONTACT");
    //             String status = rs.getString("STATUS");
    //             /*
    //              * Create Optional<Customer> from values.
    //              */
    //             return dataFactory.createCustomer(id, name, contact, status);
    //         }
    //     );
    //     /*
    //      * Probe List<Optional<Customer>> and return Optional<Customer> or
    //      * empty Optional for empty list.
    //      */
    //     return result.size() > 0? result.get(0) : 0;
    // }


    /**
     * Run query that returns all Customers with matching id in ids.
     * - query: SELECT * FROM CUSTOMER WHERE ID IN (10, 20, 30000, 40);
     * - returns Customer objects created from ResultSet rows.
     * 
     * @param ids Customer ids (WHERE IN (?ids))
     * @return Customers with matching ids.
     */
    @Override
    public Iterable<Customer> findAllCustomersById(Iterable<Long> ids) {

        /*
         * Map ids (23, 48, 96) to idsStr: "23, 48, 96"
         */
        String idsStr = StreamSupport.stream(ids.spliterator(), false)
            .map(id -> String.valueOf(id))
            .collect(Collectors.joining(", "));
        //
        var result = jdbcTemplate.queryForStream(
            /*
             * Prepare statement (ps) with "?"-augmented SQL query.
             */
            String.format("SELECT * FROM CUSTOMER WHERE ID IN (%s)", idsStr),

            /*
             * Extract values from ResultSet for each row.
             */
            (rs, rowNum) -> {
                long id = rs.getInt("ID");
                String name = rs.getString("NAME");
                String contact = rs.getString("CONTACT");
                String status = rs.getString("STATUS");
                /*
                 * Create Optional<Customer> from values.
                 */
                return dataFactory.createCustomer(id, name, contact, status);
            }
        )
        /*
         * Remove empty results from stream of Optional<Customer>,
         * map remaining from Optional<Customer> to Customer and
         * collect result.
         */
        .filter(opt -> opt.isPresent())
        .map(opt -> opt.get())
        .collect(Collectors.toList());
        //
        return result;
    }


    /**
     * Run query that returns all reservations held by a customer.
     * This is a JOIN-query between Reservation and Customer:
     * - query:
     *     SELECT RESERVATION.* FROM CUSTOMER
     *     JOIN RESERVATION ON RESERVATION.CUSTOMER_ID = CUSTOMER.ID
     *     WHERE CUSTOMER.ID = ?"
     * 
     * @param customer_id id of owning Customer.
     * @return Reservations with matching customer_id.
     */
    @Override
    public Iterable<Reservation> findReservationsByCustomerId(long customer_id) {
        //
        return jdbcTemplate.queryForStream(
            /*
             * Prepare statement (ps) with "?"-augmented SQL query.
             */
            "SELECT RESERVATION.* FROM CUSTOMER " +
            "JOIN RESERVATION ON RESERVATION.CUSTOMER_ID = CUSTOMER.ID " +
            "WHERE CUSTOMER.ID = ?",

            ps -> {
                /*
                 * Insert customer_id value of first occurence of "?" in SQL.
                 */
                ps.setInt(1, (int)customer_id);
            },

            (rs, rowNum) -> {
                /*
                 * Extract values from ResultSet.
                 */
                long rid = rs.getInt("ID");   // RESERVATION.ID
                long vehicle_id = rs.getInt("VEHICLE_ID");
                String begin = rs.getString("BEGIN");
                String end = rs.getString("END");
                String pickup = rs.getString("PICKUP");
                String dropoff = rs.getString("DROPOFF");
                String status = rs.getString("STATUS");

                /*
                 * Create Optional<Reservation> from values.
                 */
                var opt = dataFactory.createReservation(
                    rid, customer_id, vehicle_id, begin, end, pickup, dropoff, status
                );

                if(opt.isEmpty()) {
                    // log warning if no valid Reservation object could be created
                    // from database result set
                    logger.warn(String.format("dropping reservation id: %d"));
                }
                return opt;
            })
            /*
            * Remove empty results from stream of Optional<Customer>,
            * map remaining from Optional<Customer> to Customer and
            * collect result.
            */
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .collect(Collectors.toList());
    }


    /**
     * Generic method to return number of elements in Iterable<T>.
     * 
     * @param <T> Generic type of objects contained in Iterable.
     * @param iter Iterable with objects of type T.
     * @return number of elements.
     */
    @Override
    public <T> long count(Iterable<T> iter) {
        return iter instanceof Collection? ((Collection<?>) iter).size() : -1;
    }
}
