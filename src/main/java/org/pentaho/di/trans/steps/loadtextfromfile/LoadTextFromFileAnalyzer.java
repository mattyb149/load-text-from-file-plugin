package org.pentaho.di.trans.steps.loadtextfromfile;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.metaverse.api.IMetaverseNode;
import com.pentaho.metaverse.api.MetaverseAnalyzerException;
import com.pentaho.metaverse.api.MetaverseException;
import com.pentaho.metaverse.api.StepField;
import com.pentaho.metaverse.api.analyzer.kettle.step.ExternalResourceStepAnalyzer;
import com.pentaho.metaverse.api.model.IExternalResourceInfo;
import org.pentaho.di.trans.step.BaseStepMeta;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rfellows on 6/4/15.
 */
public class LoadTextFromFileAnalyzer extends ExternalResourceStepAnalyzer<LoadTextFromFileMeta> {
  @Override
  protected void customAnalyze( LoadTextFromFileMeta meta, IMetaverseNode node ) throws MetaverseAnalyzerException {
    super.customAnalyze( meta, node );
    node.setProperty( "outputFormat", meta.getOutputFormat() );
  }

  @Override
  public IMetaverseNode createResourceNode( IExternalResourceInfo resource ) throws MetaverseException {
    return createFileNode( resource.getName(), descriptor );
  }

  @Override
  public String getResourceInputNodeType() {
    return DictionaryConst.NODE_TYPE_FILE_FIELD;
  }

  @Override
  public String getResourceOutputNodeType() {
    return null;
  }

  @Override
  public boolean isOutput() {
    return false;
  }

  @Override
  public boolean isInput() {
    return true;
  }

  @Override
  protected Set<StepField> getUsedFields( LoadTextFromFileMeta meta ) {
    Set<StepField> usedFields = new HashSet<StepField>();
    if ( meta.getIsInFields() ) {
      usedFields.addAll( createStepFields( meta.getFilenameField(), getInputs() ) );
    }
    return usedFields;
  }

  @Override
  public Set<Class<? extends BaseStepMeta>> getSupportedSteps() {
    Set<Class<? extends BaseStepMeta>> supportedSteps = new HashSet<Class<? extends BaseStepMeta>>();
    supportedSteps.add( LoadTextFromFileMeta.class );
    return supportedSteps;
  }
}
