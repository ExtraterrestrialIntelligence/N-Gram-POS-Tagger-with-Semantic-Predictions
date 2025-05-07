This folder contains results and associated data files for my solutions to the assignments. In it you will find:

Overall performance statistics:
	default_out.txt -> Accuracy for just default tagger
	305_out.txt -> Accuracy for unigram and bigram tagger and overall
		time to calculate both (not broken out separately)
	575_out.txt -> Accuracy and total time for quad tagger with jackknife

	NOTE: Accuracy is Correct/Incorrect/Percent Correct


Data files:
	The results from each of the 4 runs
		default [default.txt]
		unigram [unigram.txt]
		bigram [bigram.txt]
		quadgram [quadgram_jackknife.txt]

	Note: Each row is Word/Correct Tag/Predicted Tag/Match
