package flexjson;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Comparator that uses an array of property-names to look up the Strings (Map keys)
 * being compared to order them based on their position in that property-name array.
 * The {@link BeanPropertySerializationOrder} annotation has provides the property-name array (order)
 * as its value in the bean that is serialized.  
 * 
 * @author Ted Keilman
 */
public class BeanPropertyComparator implements Comparator<String> {

    private List<String> propOrderList;

    public BeanPropertyComparator( String[] propOrder ) {
        super();
        if ( propOrder.length > 0 ) {
            propOrderList = Arrays.asList( propOrder );
        }
    }

    /**
     * The objects (key1 and key2) are compared using their position in the property order list.
     */
    public int compare( String key1, String key2 ) {

        return propOrderList.indexOf( key1 ) - propOrderList.indexOf( key2 );
    }

}