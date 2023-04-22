package mars;
import com.formdev.flatlaf.FlatDarkLaf;

public class MarsDark1
	extends FlatDarkLaf
{
	public static final String NAME = "MarsDark1";

	public static boolean setup() {
		return setup( new MarsDark1() );
	}

	public static void installLafInfo() {
		installLafInfo( NAME, MarsDark1.class );
	}

	@Override
	public String getName() {
		return NAME;
	}
}
