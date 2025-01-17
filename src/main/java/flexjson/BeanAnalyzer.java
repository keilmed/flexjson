package flexjson;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class BeanAnalyzer {

    private static ThreadLocal<Map<Class<?>,BeanAnalyzer>> cache = new ThreadLocal<Map<Class<?>,BeanAnalyzer>>();

    private Class<?> clazz;
    private BeanAnalyzer superBean;
    private Map<String,BeanProperty> properties;

    public static BeanAnalyzer analyze( Class<?> clazz ) {
        if( cache.get() == null ) cache.set( new HashMap<Class<?>,BeanAnalyzer>() );
        if( clazz == null ) return null;
        if( !cache.get().containsKey(clazz) ) {
            cache.get().put( clazz, new BeanAnalyzer(clazz) );
        }
        return cache.get().get( clazz );
    }

    public static void clearCache() {
        cache.remove();
    }

    protected BeanAnalyzer(Class<?> clazz) {
        this.clazz = clazz;
        superBean = analyze( clazz.getSuperclass() );
        populateProperties();
    }

    private void populateProperties() {
        properties = new TreeMap<String,BeanProperty>();

        for( Field publicProperties : clazz.getDeclaredFields() ) {
            int modifiers = publicProperties.getModifiers();
            if( Modifier.isStatic( modifiers ) ) continue;
            if( !properties.containsKey( publicProperties.getName() ) ) {
                properties.put( publicProperties.getName(), new BeanProperty( publicProperties, this ) );
            }
        }
        
        for( Method method : clazz.getDeclaredMethods() ) {
            int modifiers = method.getModifiers();
            if( Modifier.isStatic(modifiers) ) continue;

            int numberOfArgs = method.getParameterTypes().length;
            String name = method.getName();
            if( name.length() <= 3 && !name.startsWith("is") ) continue;
            
            if( numberOfArgs == 0 ) {
                if( name.startsWith("get") ) {
                    String property = uncapitalize( name.substring(3) );
                    if( !properties.containsKey( property ) ) {
                        properties.put( property, new BeanProperty(property, this) );
                    }
                    properties.get( property ).setReadMethod( method );
                } else if( name.startsWith("is") ) {
                    String property = uncapitalize( name.substring(2) );
                    if( !properties.containsKey( property ) ) {
                        properties.put( property, new BeanProperty(property, this) );
                    }
                    properties.get( property ).setReadMethod(method);
                }
            } else if( numberOfArgs == 1 ) {
                if( name.startsWith("set") ) {
                    String property = uncapitalize( name.substring(3) );
                    if( !properties.containsKey( property ) ) {
                        properties.put( property, new BeanProperty(property, this) );
                    }
                    properties.get( property ).addWriteMethod(method);
                }
            }
        }

        for( Iterator<BeanProperty> i = properties.values().iterator(); i.hasNext(); ) {
            BeanProperty property = i.next();
            if( property.isNonProperty() ) {
                i.remove();
            }
        }
    }

    public BeanAnalyzer getSuperBean() {
        return superBean;
    }

    private String uncapitalize( String value ) {
        if( value.length() < 2 ) {
             return value.toLowerCase();
        } else if( Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1)) ) {
            return value;
        } else {
            return Character.toLowerCase( value.charAt(0) ) + value.substring(1);
        }
    }

    public BeanProperty getProperty(String name) {
        BeanAnalyzer current = this;
        while( current != null ) {
            BeanProperty property = current.properties.get( name );
            if( property != null ) return property;
            current = current.superBean;
        }
        return null;
    }

    public Collection<BeanProperty> getProperties() {

        Map<String,BeanProperty> properties = new TreeMap<String,BeanProperty>(this.properties);

        BeanAnalyzer current = this.superBean;
        while( current != null ) {
            merge( properties, current.properties );
            current = current.superBean;
        }

        // If the bean has an annotation to control the order of the bean property values, apply that order by
        // using a new Map that employs a custom Comparator and return the resulting ordered property values.
        if ( clazz.isAnnotationPresent( BeanPropertySerializationOrder.class ) ) {

            String[] propOrder = ( (BeanPropertySerializationOrder) clazz.getAnnotation( BeanPropertySerializationOrder.class ) ).propertyOrder();
            Map<String,BeanProperty> orderedProperties = new TreeMap<String,BeanProperty>( new BeanPropertyComparator( propOrder ) );
            orderedProperties.putAll( properties );
            return orderedProperties.values();
        }

        // Property values are not being custom ordered. They will be returned in natural order (alphabetical).
        return properties.values();
    }

    private void merge(Map<String, BeanProperty> destination, Map<String, BeanProperty> source) {
        for( String key : source.keySet() ) {
            if( !destination.containsKey( key ) ) {
                destination.put( key, source.get(key) );
            }
        }
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name) || (superBean != null && superBean.hasProperty(name));
    }

    protected Field getDeclaredField(String name) {
        try {
            return clazz.getDeclaredField( name );
        } catch (NoSuchFieldException e) {
            // ignore field does not exist.
            return null;
        }
    }
}
