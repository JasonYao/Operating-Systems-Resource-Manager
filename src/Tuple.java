/**
 * Tuple object class
 */
public class Tuple
{
    // Tuple attributes
    Resource resource;
    int amountOfResource;

    public Tuple(Resource resource, int amountOfResource)
    {
        setResource(resource);
        setAmountOfResource(amountOfResource);
    } // End of the Tuple object constructor

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public int getAmountOfResource() {
        return amountOfResource;
    }

    public void setAmountOfResource(int amountOfResource) {
        this.amountOfResource = amountOfResource;
    }
} // End of the tuple class
