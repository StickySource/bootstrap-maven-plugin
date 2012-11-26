package net.stickycode.plugins.bootstrap;

import java.lang.reflect.Field;

import net.stickycode.reflector.Fields;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.sonatype.aether.connector.wagon.WagonProvider;

public class ManualWagonProvider
    implements WagonProvider {

  public Wagon lookup(String roleHint)
      throws Exception
  {
    if ("http".equals(roleHint))
    {
      LightweightHttpWagon lightweightHttpWagon = new LightweightHttpWagon();
      Field field = Fields.find(LightweightHttpWagon.class, "authenticator");
      Fields.set(lightweightHttpWagon, field, new LightweightHttpWagonAuthenticator());
      return lightweightHttpWagon;
    }
    return null;
  }

  public void release(Wagon wagon)
  {

  }

}
