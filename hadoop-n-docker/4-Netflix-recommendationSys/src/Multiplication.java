import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class Multiplication {
	public static class MultiplicationMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {


		Map<Integer, List<MovieRelation>> movieRelationMap = new HashMap<>();
		Map<Integer, Integer> denominator = new HashMap<>();

		@Override
		protected void setup(Context context) throws IOException {
			Configuration conf = context.getConfiguration();
			String filePath = conf.get("coOccurrencePath", "/coOccurrenceMatrix/part-r-00000");
			Path pt = new Path(filePath);
			FileSystem fs = FileSystem.get(conf);
			// BufferReader br <----> StringBuilder sb
                        // Before the actual mapper starts to crunch the input piece by piece 
                        // the Setup process will read in the specified input file line by line and figure out the denominator map.
                        BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(pt)));
			String line = br.readLine();
			
			while(line != null) {
				//movieA: movieB \t relation
				String[] tokens = line.toString().trim().split("\t");
				String[] movies = tokens[0].split(":");
				
				int movie1 = Integer.parseInt(movies[0]);       // movieA id
				int movie2 = Integer.parseInt(movies[1]);       // movieB id
				int relation = Integer.parseInt(tokens[1]);     // co-occurrence count
			        
                                // created a basic but specific datastructure - a tuple - for the purpose of convenient storage and retrieval.        
				MovieRelation movieRelation = new MovieRelation(movie1, movie2, relation);
				if(movieRelationMap.containsKey(movie1)) {
					movieRelationMap.get(movie1).add(movieRelation);
				} // group on movieA_ID, does it have to be movieA?
				else {
					List<MovieRelation> list = new ArrayList<>();
					list.add(movieRelation);
					movieRelationMap.put(movie1, list);
				}
				line = br.readLine();
			}
			br.close();
			
			for(Map.Entry<Integer, List<MovieRelation>> entry: movieRelationMap.entrySet()) {
				int sum = 0;
				for(MovieRelation relation: entry.getValue()) {
					sum += relation.getRelation();
				}
				//movieA sum(relations)
				denominator.put(entry.getKey(), sum);
                            
			} // so basically the setup process is preparing a HashMap - denominator - for use in the following Multiplication.
                          // [movieA:movieB, C1 \t movieA:movieC, C2] -------- input value
                          // -> {movieA: [(movieA: movieB, C1), (movieA: movieC, C2)]} ----- movieRelationMap
                          // -> {movieA: sum(C1, C2, C3 ... Cn)} ------denominator
			
		}

		// map method
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		        // here the mapper takes in the original input for score info, which hasn't been digested so far
                        //input user,movie,rating
			//output user:movie score
			String[] tokens = value.toString().trim().split(",");
			int user = Integer.parseInt(tokens[0]);
			int movie = Integer.parseInt(tokens[1]);
			double rating = Double.parseDouble(tokens[2]);
			
			for(MovieRelation relation : movieRelationMap.get(movie)) {
                            // movieRelation -> {movie: [(movie, movieX1, coocurrence), (movie, movieX2, cooccurrence)... ]}
				double score = rating * relation.getRelation(); // movieScore * movie-movieX coocurrence
				//normalize - now you see why it is double for score ?
				score = score/denominator.get(relation.getMovie2()); 
                                // movieScore * movie-X-cooccurrence / sum(occurrence of X)
                                // INTERPRETATION: 
                                // for the user, who watch movie, but did watch X, this score shows how likely he will like X if recommended.
				DecimalFormat df = new DecimalFormat("#.00");
				score = Double.valueOf(df.format(score));
				context.write(new Text(user + ":" + relation.getMovie2()), new DoubleWritable(score));
                                // for each movie this user watched, a score will be calculated to evaluate how similar movie and X are.
                                // during reducer process, the scores will be summed up to represent a general likelihood that the user will like X.
				// output -> user: movieX score
			}
		}
	}

	public static class MultiplicationReducer extends Reducer<Text, DoubleWritable, IntWritable, Text> {
		// reduce method
		@Override
		public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
				throws IOException, InterruptedException {
			//user:movie score
			double sum = 0;
			while(values.iterator().hasNext()) {
				sum += values.iterator().next().get();
			}
			String[] tokens = key.toString().split(":");
			int user = Integer.parseInt(tokens[0]);
			context.write(new IntWritable(user), new Text(tokens[1] + ":" + sum));
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.set("coOccurrencePath", args[0]);
		
		Job job = Job.getInstance();
		job.setMapperClass(MultiplicationMapper.class);
		job.setReducerClass(MultiplicationReducer.class);
		
		job.setJarByClass(Multiplication.class);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(DoubleWritable.class);
		
		TextInputFormat.setInputPaths(job, new Path(args[1]));
		TextOutputFormat.setOutputPath(job, new Path(args[2]));
		
		job.waitForCompletion(true);
	}
}
