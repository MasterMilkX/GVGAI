package ontology.effects.binary;

import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;
import core.content.InteractionContent;
import core.game.Game;
import core.logging.Logger;
import core.logging.Message;
import ontology.effects.Effect;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 04/11/13
 * Time: 13:25
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class ChangeResource extends Effect
{
    public String resource;
    public int resourceId;
    public int value;
    public boolean killResource;

    public ChangeResource(InteractionContent cnt)
    {
        value=1;
        resourceId = -1;
        killResource = false;
        this.parseParameters(cnt);
        resourceId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(resource);
        is_kill_effect = killResource;
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game) {
	if(sprite1 == null || sprite2 == null){
	    String[] className = this.getClass().getName().split("\\.");
	    Logger.getInstance().addMessage(new Message(Message.WARNING, "[" + className[className.length - 1]  + "] Either sprite1 or sprite2 is equal to null."));
	    return;
	}
        int numResources = sprite1.getAmountResource(resourceId);
        applyScore = false;
        if(numResources + value <= game.getResourceLimit(resourceId))
        {
            sprite1.modifyResource(resourceId, value);
            applyScore = true;

            if(killResource)
                //boolean variable set to true, as the sprite was transformed
                game.killSprite(sprite2, true);
        }
    }
}
