package brooklyn.entity.rebind;

import java.util.List;

import brooklyn.entity.Application;
import brooklyn.mementos.BrooklynMemento;
import brooklyn.mementos.BrooklynMementoPersister;

public interface RebindManager {
    
    // FIXME Should we be calling managementContext.getRebindManager().rebind, using a
    // new empty instance of managementContext?
    //
    // Or is that a risky API because you could call it on a non-empty managementContext?
    
    public void setPersister(BrooklynMementoPersister persister);

    public BrooklynMemento getMemento();

    public List<Application> rebind(final BrooklynMemento memento);
    
    public List<Application> rebind(final BrooklynMemento memento, ClassLoader classLoader);

    public ChangeListener getChangeListener();
}
