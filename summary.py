# -*- coding: utf-8 -*-
"""
Created on Wed May 24 11:05:16 2017

@author: pudutta
"""


from __future__ import division
import re


class SummaryTool(object):

   
    def split_content_to_sentences(self, content):
        content = content.replace("\n", ". ")
        return content.split(". ")

    
    def split_content_to_paragraphs(self, content):
        return content.split("\n\n")

   
    def sentences_intersection(self, sent1, sent2):
        s1 = set(sent1.split(" "))
        s2 = set(sent2.split(" "))

        
        if (len(s1) + len(s2)) == 0:
            return 0

       
        return len(s1.intersection(s2)) / ((len(s1) + len(s2)) / 2)

  
    def format_sentence(self, sentence):
        sentence = re.sub(r'\W+', '', sentence)
        return sentence

   
    def get_senteces_ranks(self, content):

        
        sentences = self.split_content_to_sentences(content)

        
        n = len(sentences)
        values = [[0 for x in range(n)] for x in range(n)]
        for i in range(0, n):
            for j in range(0, n):
                values[i][j] = self.sentences_intersection(sentences[i], sentences[j])

        
        sentences_dic = {}
        for i in range(0, n):
            score = 0
            for j in range(0, n):
                if i == j:
                    continue
                score += values[i][j]
            sentences_dic[self.format_sentence(sentences[i])] = score
        return sentences_dic

    
    def get_best_sentence(self, paragraph, sentences_dic):

        
        sentences = self.split_content_to_sentences(paragraph)

       
        if len(sentences) < 2:
            return ""

        
        best_sentence = ""
        max_value = 0
        for s in sentences:
            strip_s = self.format_sentence(s)
            if strip_s:
                if sentences_dic[strip_s] > max_value:
                    max_value = sentences_dic[strip_s]
                    best_sentence = s

        return best_sentence

   
    def get_summary(self, title, content, sentences_dic):

        # Split the content into paragraphs
        paragraphs = self.split_content_to_paragraphs(content)

        # Add the title
        summary = []
        summary.append(title.strip())
        summary.append("")

        # Add the best sentence from each paragraph
        for p in paragraphs:
            sentence = self.get_best_sentence(p, sentences_dic).strip()
            if sentence:
                summary.append(sentence)

        return ("\n").join(summary)


# Main method, just run "python summary_tool.py"
def main():

   

    title = """
    Automatic summarization
    """

    content = """
    Many machine learning algorithms require the
input to be represented as a fixed-length feature
vector. When it comes to texts, one of the most
common fixed-length features is bag-of-words.
Despite their popularity, bag-of-words features
have two major weaknesses: they lose the ordering
of the words and they also ignore semantics
of the words. For example, “powerful,” “strong”
and “Paris” are equally distant. In this paper, we
propose Paragraph Vector, an unsupervised algorithm
that learns fixed-length feature representations
from variable-length pieces of texts, such as
sentences, paragraphs, and documents. Our algorithm
represents each document by a dense vector
which is trained to predict words in the document.
Its construction gives our algorithm the
potential to overcome the weaknesses of bag-ofwords
models. Empirical results show that Paragraph
Vectors outperform bag-of-words models
as well as other techniques for text representations.
Finally, we achieve new state-of-the-art results
on several text classification and sentiment analysis 
tasks
   """

    # Create a SummaryTool object
    st = SummaryTool()

    # Build the sentences dictionary
    sentences_dic = st.get_senteces_ranks(content)

    # Build the summary with the sentences dictionary
    summary = st.get_summary(title, content, sentences_dic)

    # Print the summary
    print (summary)

    # Print the ratio between the summary length and the original length
    print ("")
    print ("Original Length %s" % (len(title) + len(content)))
    print ("Summary Length %s" % len(summary))
    print ("Summary Ratio: %s" % (100 - (100 * (len(summary) / (len(title) + len(content))))))


if __name__ == '__main__':
    main()