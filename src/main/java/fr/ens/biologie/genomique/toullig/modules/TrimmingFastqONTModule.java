
package fr.ens.biologie.genomique.toullig.modules;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.*;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.toullig.trimming.TrimFastq;

import java.io.File;
import java.io.IOException;
import java.util.Set;

// The "@LocalOnly" annotation means that the Eoulsan workflow engine will
// only use this step in local mode. The two other annotations are "@HadoopOnly"
// and "@HadoopCompatible" when a step can be executed in local or Hadoop mode.
@LocalOnly
public class TrimmingFastqONTModule extends AbstractModule {
  @Override
  public String getName() {
    // This method return the name of the step
    // We don't use gsnap as module name as it already exists in Eoulsan
    return "trimmingfastqont";
  }

  @Override
  public Version getVersion() {
    // This method return the version of the module
    return new Version(0, 1, 0);
  }

  @Override
  public TaskResult execute(TaskContext context, TaskStatus status) {
    // The context object had many useful method for writing a Module
    // (e.g. access to file to process, the workflow description, the
    // logger...).

    // The status object contains methods to inform the workflow about the
    // progress of the task. The status object is also used to create the
    // TaskResult objects.

    try {

      // Get the sam Data
      final Data samData = context.getInputData(DataFormats.MAPPER_RESULTS_SAM);

      // Get the fastq Data
      final Data fastqData = context.getInputData(DataFormats.READS_FASTQ);

      // Get the adaptor Data
      final Data adaptorData = context.getInputData(DataFormatRegistry
          .getInstance().getDataFormatFromName("cutadapt_adapter"));

      // Get the sam File
      final File samFile = samData.getDataFile().toFile();

      // Get the fasqt File
      final File fastqFile = fastqData.getDataFile().toFile();

      // Get the adaptor File
      final File adaptorFile = adaptorData.getDataFile().toFile();

      // Get the fastq output File
      final File fastqOutputFile =
          context.getOutputData(DataFormats.READS_FASTQ, fastqData)
              .getDataFile().toFile();

      // Get the fastq output File
      final File workDir = samData.getDataFile().toFile().getParentFile();

      try {

        // call the constructor with the arguments
        TrimFastq trim = new TrimFastq(samFile, fastqFile, adaptorFile,
            fastqOutputFile, workDir, trimmer, mode);

        // Set the stats for cutadapt to display
        if (this.stats) {
          trim.setProcessStatsCutadapt();
        }

        // Set the threshold for Side-Window method
        if (this.thresholdSW != 0) {
          trim.setThresholdSideWindow(this.thresholdSW);
        }

        // Set the length window for Side-Window method
        if (this.lengthWindowsSW != 0) {
          trim.setLengthWindowSideWindow(this.lengthWindowsSW);
        }

        // Set the number of add index outlier for Perfect method
        if (this.addIndexOutlier != 0) {
          trim.setAddIndexOutlier(this.addIndexOutlier);
        }

        // Set the error rate for Cutadapt trimmer
        if (this.errorRateCutadapt != 0) {
          trim.setErrorRateCutadapt(this.errorRateCutadapt);
        }

        // Set the seed Mismatches for Trimmomatic trimmer
        if (this.seedMismatchesTrimmomatic != 0) {
          trim.setSeedMismatchesTrimmomatic(this.seedMismatchesTrimmomatic);
        }

        // Set the palindrome Clip Threshold for Trimmomatic trimmer
        if (this.palindromeClipThresholdTrimmomatic != 0) {
          trim.setPalindromeClipThresholdTrimmomatic(
              this.palindromeClipThresholdTrimmomatic);
        }

        // Set the simple Clip Threshold for Trimmomatic trimmer
        if (this.simpleClipThreshold != 0) {
          trim.setSimpleClipThreshold(this.simpleClipThreshold);
        }

        // execution
        trim.execution();

        // Create a success TaskResult object and return this object to the
        // workflow
        return status.createTaskResult();

      } catch (IOException e) {

        // If an exception occurs while running Gsnap, return a error TaskResult
        // object with the exception that cause the error
        return status.createTaskResult(e);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return status.createTaskResult(e);
    }
  }

  @Override
  public InputPorts getInputPorts() {

    // This method define the 3 input ports of the module
    // This method is called by the workflow after the configure() method. So
    // the number and type of the input port can change against the
    // configuration of the module

    final InputPortsBuilder builder = new InputPortsBuilder();

    builder.addPort("sam", DataFormats.MAPPER_RESULTS_SAM);
    builder.addPort("reads", DataFormats.READS_FASTQ);
    builder.addPort("adaptor", DataFormats.DUMMY_TXT);

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    // This method define the output ports of the module
    // This method is called by the workflow after the configure() method. So
    // the number and type of the output port can change against the
    // configuration of the module

    return OutputPortsBuilder.singleOutputPort(DataFormats.READS_FASTQ);
  }

  private String trimmer = "cutadapt";
  private String mode = "P";
  private boolean stats = false;
  private double thresholdSW = 0;
  private int lengthWindowsSW = 0;
  private int addIndexOutlier = 0;
  private double errorRateCutadapt = 0;
  private int seedMismatchesTrimmomatic = 0;
  private int palindromeClipThresholdTrimmomatic = 0;
  private int simpleClipThreshold = 0;

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // This method allow to configure the module
    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "trimmer.name":
        trimmer = p.getStringValue();
        if (!trimmer.equals("cutadapt") || !trimmer.equals("trimmomatic")) {
          Modules.unknownParameter(context, p);
        }
        break;

      case "trimmer.mode":
        mode = p.getStringValue();
        if (!mode.equals("P") || !mode.equals("SW")) {
          Modules.unknownParameter(context, p);
        }
        break;

      case "trimmer.stats":
        stats = p.getBooleanValue();
        if (!stats || stats) {
          Modules.unknownParameter(context, p);
        }
        break;

      case "SW.thresholdSW":
        thresholdSW = p.getDoubleValue();
        break;

      case "SW.lengthWindowsSW":
        lengthWindowsSW = p.getIntValue();
        break;

      case "P.addIndexOutlier":
        addIndexOutlier = p.getIntValue();
        break;

      case "cutadapt.errorRateCutadapt":
        errorRateCutadapt = p.getDoubleValue();
        break;

      case "trimmomatic.seedMismatchesTrimmomatic":
        seedMismatchesTrimmomatic = p.getIntValue();
        break;

      case "trimmomatic.palindromeClipThresholdTrimmomatic":
        palindromeClipThresholdTrimmomatic = p.getIntValue();
        break;

      case "trimmomatic.simpleClipThreshold":
        simpleClipThreshold = p.getIntValue();
        break;

      default:
        Modules.unknownParameter(context, p);
        break;
      }
    }
  }

  @Override
  public ParallelizationMode getParallelizationMode() {
    // The mapper programs can use multithreading, so we don't let here Eoulsan
    // run several mapping at the same time by using OWN_PARALLELIZATION mode
    // instead of STANDARD parallelization mode
    return ParallelizationMode.OWN_PARALLELIZATION;
  }

}
