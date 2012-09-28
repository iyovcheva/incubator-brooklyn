package brooklyn.entity.rebind;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.Location;
import brooklyn.location.basic.AbstractLocation;
import brooklyn.mementos.LocationMemento;
import brooklyn.util.flags.FlagUtils;

import com.google.common.collect.ImmutableMap;

public class BasicLocationRebindSupport implements RebindSupport<LocationMemento> {

	// FIXME Filter passwords from log messages!
	
    protected static final Logger LOG = LoggerFactory.getLogger(BasicLocationRebindSupport.class);
    
    private final AbstractLocation location;
    
    public BasicLocationRebindSupport(AbstractLocation location) {
        this.location = location;
    }
    
    @Override
    public LocationMemento getMemento() {
        return getMementoWithProperties(Collections.<String,Object>emptyMap());
    }

    protected LocationMemento getMementoWithProperties(Map<String,?> props) {
        LocationMemento memento = BasicLocationMemento.builder().from(location).customProperties(props).build();
    	if (LOG.isTraceEnabled()) LOG.trace("Creating memento for location {}({}): displayName={}; locationProperties={}; " +
    			"flags={}; customProperties={}; parent={}; children={}",
    			new Object[] {memento.getType(), memento.getId(), memento.getDisplayName(), memento.getLocationProperties(), 
				memento.getFlags(), memento.getCustomProperties(), memento.getParent(), memento.getChildren()});
    	return memento;
    }

    @Override
    public void reconstruct(RebindContext rebindContext, LocationMemento memento) {
    	if (LOG.isTraceEnabled()) LOG.trace("Reconstructing location {}({}): displayName={}; parent={}; children={}; " +
    			"locationProperties={}; flags={}; locationReferenceFlags={}; customProperties={}",
    			new Object[] {memento.getType(), memento.getId(), memento.getDisplayName(), memento.getParent(), 
    			memento.getChildren(), memento.getLocationProperties(), 
				memento.getFlags(), memento.getLocationReferenceFlags(), memento.getCustomProperties()});

    	// Note that the flags have been set in the constructor
        location.setName(memento.getDisplayName());
        location.addLeftoverProperties(memento.getLocationProperties());
        
        // Do late-binding of flags that are references to other locations
        for (String flagName : memento.getLocationReferenceFlags()) {
            Field field = FlagUtils.findFieldForFlag(flagName, location);
            Class<?> fieldType = field.getType();
            Object transformedValue = memento.getFlags().get(flagName);
            Object restoredValue = MementoTransformer.transformIdsToLocations(rebindContext, transformedValue, fieldType);
            FlagUtils.setFieldsFromFlags(ImmutableMap.of(flagName, restoredValue), location);
        }

        setParent(rebindContext, memento);
        addChildren(rebindContext, memento);
        
        doReconsruct(rebindContext, memento);
    }

    @Override
    public void rebind(RebindContext rebindContext, LocationMemento memento) {
    	if (LOG.isTraceEnabled()) LOG.trace("Rebinding location {}({})");
    	
        doRebind(rebindContext, memento);
    }

    @Override
    public void managed() {
        if (LOG.isTraceEnabled()) LOG.trace("Managed entity {}({})", new Object[] {location.getClass(), location.getId()});
    }

    protected void addChildren(RebindContext rebindContext, LocationMemento memento) {
        for (String childId : memento.getChildren()) {
            Location child = rebindContext.getLocation(childId);
            if (child != null) {
            	location.addChildLocation(child);
            } else {
            	LOG.warn("Ignoring child {} of location {}({}), as cannot be found", new Object[] {childId, memento.getType(), memento.getId()});
            }
        }
    }

    protected void setParent(RebindContext rebindContext, LocationMemento memento) {
        Location parent = (memento.getParent() != null) ? rebindContext.getLocation(memento.getParent()) : null;
        if (parent != null) {
            location.setParentLocation(parent);
        } else if (memento.getParent() != null) {
        	LOG.warn("Ignoring parent {} of location {}({}), as cannot be found", new Object[] {memento.getParent(), memento.getType(), memento.getId()});
        }
    }
    
    /**
     * For overriding, to give custom reconsruct behaviour.
     */
    protected void doReconsruct(RebindContext rebindContext, LocationMemento memento) {
        // default is no-op
    }
    
    /**
     * For overriding, to give custom rebind behaviour.
     */
    protected void doRebind(RebindContext rebindContext, LocationMemento memento) {
        // default is no-op
    }
}
