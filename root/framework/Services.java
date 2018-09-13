package framework;

import java.util.HashMap;

public class Services {
    private static HashMap<Class, Object> services = new HashMap<>();

    public static <TServiceInterface, TService extends TServiceInterface> void register(Class<TServiceInterface> type, TService instance)
    {
        if (type == null)
            throw new IllegalArgumentException("type cannot be null");
        if (instance == null)
            throw new IllegalArgumentException("instance cannot be null");
        Services.services.put(type, instance);
    }

    public static <TServiceInterface> TServiceInterface retrieve(Class<TServiceInterface> type)
    {
        if (type == null)
            throw new IllegalArgumentException("type cannot be null");
        return (TServiceInterface) Services.services.get(type);
    }

    public static void clear()
    {
        Services.services.clear();
    }
}
