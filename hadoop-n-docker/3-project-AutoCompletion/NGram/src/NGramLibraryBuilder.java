

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class NGramLibraryBuilder {

	public static class NGramMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
		
		int noGram;
	        //passed in from context.getConfiguration(), which is created in Driver class and connected with hadoop env

		@Override
		public void setup(Context context) {
			Configuration conf = context.getConfiguration();
			noGram = conf.getInt("noGram", 5); 
                        // here 5 is given as a default value, "noGram" can be passed via console
		}
		
		//map method
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
                        // in map() the LongWritable key stands for the id of the value, such as when the value is the text content in a book
                        // Mapred will take care of the inputs under the given path. What about inputs under different dir?
			String line = value.toString();

			line = line.trim().toLowerCase();
			line = line.replaceAll("[^a-z]+", " ");
                        //regex in java
			String words[] = line.split("\\s+"); //split by ' ', '\t', '\n', etc. 
                        // line.split(" ") also works
			
			if(words.length < 2) {
				return;
			}
			
                        // I'd say the following part is the gist of Ngram model
			StringBuilder sb;
			for (int i = 0; i < words.length-1; i++) {
				sb = new StringBuilder();
                                // with noGram == 5, j is from 0 - 4]
				for (int j = 0;  i + j < words.length && j < noGram; j++) {
					sb.append(" ");
					sb.append(words[i + j]);
                                        // output ("\s" + word1 + "\s" + word2 + ... word5, 1)
					context.write(new Text(sb.toString().trim()), new IntWritable(1));
				}
			}
		}
	}

	public static class NGramReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		//reduce method
		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
                        // Reducer completely takes in the output from Shuffle, same key mapping an iterable storing values
			int sum = 0;
			for (IntWritable v : values) {
				sum += v.get();
			}
			context.write(key, new IntWritable(sum));
		}
	}
}
