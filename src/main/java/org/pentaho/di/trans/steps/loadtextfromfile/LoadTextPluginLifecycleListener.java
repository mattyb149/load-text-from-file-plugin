package org.pentaho.di.trans.steps.loadtextfromfile;

import org.pentaho.di.core.annotations.LifecyclePlugin;
import org.pentaho.di.core.lifecycle.LifeEventHandler;
import org.pentaho.di.core.lifecycle.LifecycleException;
import org.pentaho.di.core.lifecycle.LifecycleListener;
import org.pentaho.platform.engine.core.system.PentahoSystem;

/**
 * Created by rfellows on 6/4/15.
 */
@LifecyclePlugin( id = "LoadTextFromFilePlugin", name = "LoadTextFromFilePlugin" )
public class LoadTextPluginLifecycleListener implements LifecycleListener {
  LoadTextFromFileAnalyzer analyzer;
  LoadTextFromFileExternalResourceConsumer consumer;
  @Override
  public void onStart( LifeEventHandler lifeEventHandler ) throws LifecycleException {
    analyzer = new LoadTextFromFileAnalyzer();
    consumer = new LoadTextFromFileExternalResourceConsumer();
    analyzer.setExternalResourceConsumer( consumer );
    PentahoSystem.registerObject( analyzer );
  }

  @Override
  public void onExit( LifeEventHandler lifeEventHandler ) throws LifecycleException {
  }
}
