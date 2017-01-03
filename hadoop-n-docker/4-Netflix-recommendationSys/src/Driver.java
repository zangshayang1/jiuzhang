
public class Driver {
	public static void main(String[] args) throws Exception {
		
                // $ hadoop jar recommender.jar Driver /input /dataDividedByUser /coOccurrenceMatrix /DataDividedByMovie /Multiplication /coOccurrenceMatrix/part-r-00000 /input/userRating.txt /MovieTitle/movieTitles.txt /Recommender
            
                DataDividerByUser dataDividerByUser = new DataDividerByUser();
		CoOccurrenceMatrixGenerator coOccurrenceMatrixGenerator = new CoOccurrenceMatrixGenerator();
		Multiplication multiplication = new Multiplication();
		RecommenderListGenerator generator = new RecommenderListGenerator();
		
                /*
                   args[0] -> /input
                   args[1] -> /dataDividedByUser
                   args[2] -> /coOccurrenceMatrix
                   args[3] -> /DataDividedByMovie
                   args[4] -> /Multiplication
                   args[5] -> /coOccurrenceMatrix/part-r-00000
                   args[6] -> /input/userRating.txt
                   args[7] -> /MovieTitle/movieTitles.txt
                   args[8] -> /Recommender
                */

		String[] path1 = {args[0], args[1]};
		String[] path2 = {args[1], args[2]};
		String[] path3 = {args[5], args[0], args[4]};
		String[] path4 = {args[6], args[7], args[4], args[8]};
		
		dataDividerByUser.main(path1);  //-> /input /dataDividedByUser
		coOccurrenceMatrixGenerator.main(path2);  //-> /dataDividedByUser /coOccurrenceMatrix
		multiplication.main(path3);  //-> /coOccurrenceMatrix/part-r-00000 /input /Multiplication (you specify part-r-00000 for setup to take up
                                                                                                        // and give raw_input again to extract score info.)
		generator.main(path4);  //-> /input/userRating.txt /MovieTitle/movieTitles.txt /Multiplication /Recommender
	}

}
