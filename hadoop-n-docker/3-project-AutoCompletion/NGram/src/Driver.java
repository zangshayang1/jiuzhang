import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.db.DBConfiguration;
import org.apache.hadoop.mapreduce.lib.db.DBOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;


public class Driver {

	public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
		Configuration conf1 = new Configuration();
		conf1.set("textinputformat.record.delimiter", ".");
		conf1.set("noGram", args[2]);
		
		//First Job 
	        Job job1 = Job.getInstance(conf1);
	        job1.setJobName("NGram");
	        job1.setJarByClass(Driver.class);

	        job1.setMapperClass(NGramLibraryBuilder.NGramMapper.class);
	        job1.setReducerClass(NGramLibraryBuilder.NGramReducer.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);

		TextInputFormat.setInputPaths(job1, new Path(args[0]));
		TextOutputFormat.setOutputPath(job1, new Path(args[1]));
	    
  	        job1.waitForCompletion(true);
                //$ hadoop com.java.sun.tools.javac.Main *.java -> compile in hadoop
                //$ jar cf ngram.jar *.class -> create file.jar to run
                //$ hadoop jar ngram.jar Driver input /output 2 3 4
                //2 -> ngram_size
                //3 -> threshold: if the word appearing frequency is lower than threshold it will be left out
                //4 -> following_word_size

	        //Second Job 
	        Configuration conf2 = new Configuration();
	        conf2.set("threashold", args[3]);
	        conf2.set("n", args[4]);
	        DBConfiguration.configureDB(conf2,
	    	     "com.mysql.jdbc.Driver",   // java database connectivity driver class
	    	     "jdbc:mysql://17.245.76.166:8889/test", // host ip port
	    	     "root",    // user name
	    	     ""); //password
		
	        Job job2 = Job.getInstance(conf2);
	        job2.setJobName("LanguageModel");
	        job2.setJarByClass(Driver.class);
	    
	        job2.addArchiveToClassPath(new Path("/mysql/mysql-connector-java-5.0.8-bin.jar"));
                // jar stands for java archive file, which packs all the classes and metadata that make the API
	        job2.setMapOutputKeyClass(Text.class);
	        job2.setMapOutputValueClass(Text.class);
	        job2.setOutputKeyClass(Text.class);
	        job2.setOutputValueClass(NullWritable.class);

	        job2.setMapperClass(LanguageModel.Map.class);
	        job2.setReducerClass(LanguageModel.Reduce.class);

	        job2.setInputFormatClass(TextInputFormat.class);
		job2.setOutputFormatClass(DBOutputFormat.class);
	    
		DBOutputFormat.setOutput(
			     job2,
			     "output",    // output table name
			     new String[] { "starting_phrase", "following_word", "count" }   //table columns
			     );
		
  	        //Path name for this job should match first job's output path name
		TextInputFormat.setInputPaths(job2, new Path(args[1]));
		System.exit(job2.waitForCompletion(true)?0:1);

	}

}
