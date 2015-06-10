package org.pentaho.di.trans.steps.loadtextfromfile;

import org.pentaho.metaverse.api.IAnalysisContext;
import org.pentaho.metaverse.api.analyzer.kettle.step.BaseStepExternalResourceConsumer;
import org.pentaho.metaverse.api.model.ExternalResourceInfoFactory;
import org.pentaho.metaverse.api.model.IExternalResourceInfo;
import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by rfellows on 6/4/15.
 */
public class LoadTextFromFileExternalResourceConsumer extends
  BaseStepExternalResourceConsumer<LoadTextFromFile, LoadTextFromFileMeta> {

  @Override
  public boolean isDataDriven( LoadTextFromFileMeta meta ) {
    return meta.getIsInFields();
  }

  @Override
  public Collection<IExternalResourceInfo> getResourcesFromMeta( LoadTextFromFileMeta meta, IAnalysisContext context ) {
    Collection<IExternalResourceInfo> resources = new LinkedList<IExternalResourceInfo>();
    if ( !isDataDriven( meta ) ) {
      StepMeta parentStepMeta = meta.getParentStepMeta();
      if ( parentStepMeta != null ) {
        TransMeta parentTransMeta = parentStepMeta.getParentTransMeta();
        if ( parentTransMeta != null ) {
          FileInputList paths = meta.getFiles( parentTransMeta );
          if ( paths != null ) {
            resources = new ArrayList<IExternalResourceInfo>( paths.nrOfFiles() );
            for ( String path : paths.getFileStrings() ) {
              if ( !Const.isEmpty( path ) ) {
                try {
                  IExternalResourceInfo resource = ExternalResourceInfoFactory
                    .createFileResource( KettleVFS.getFileObject( path ), true );
                  if ( resource != null ) {
                    resources.add( resource );
                  } else {
                    throw new KettleFileException( "Error getting file resource!" );
                  }
                } catch ( KettleFileException kfe ) {
                  // TODO throw or ignore?
                }
              }
            }
          }
        }
      }
    }
    return resources;
  }

  @Override
  public Collection<IExternalResourceInfo> getResourcesFromRow( LoadTextFromFile step, RowMetaInterface rowMeta,
                                                                Object[] row ) {
    Collection<IExternalResourceInfo> resources = new LinkedList<IExternalResourceInfo>();
    // For some reason the step doesn't return the StepMetaInterface directly, so go around it
    LoadTextFromFileMeta meta = (LoadTextFromFileMeta) step.getStepMetaInterface();
    if ( meta == null ) {
      meta = (LoadTextFromFileMeta) step.getStepMeta().getStepMetaInterface();
    }
    try {
      String filename = meta == null ? null : rowMeta.getString( row, meta.getFilenameField(), null );
      if ( !Const.isEmpty( filename ) ) {
        FileObject fileObject = KettleVFS.getFileObject( filename );
        resources.add( ExternalResourceInfoFactory.createFileResource( fileObject, true ) );
      }
    } catch ( KettleException e ) {
      e.printStackTrace();
    }
    return resources;
  }

  @Override
  public Class<LoadTextFromFileMeta> getMetaClass() {
    return LoadTextFromFileMeta.class;
  }
}
