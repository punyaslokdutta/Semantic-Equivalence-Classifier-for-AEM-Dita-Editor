# -*- coding: utf-8 -*-
"""
Created on Sat May 20 19:09:46 2017

@author: pudutta
"""

from xlwt import Workbook
wb=Workbook()
sheet1=wb.add_sheet('Sheet 1')
cols = ["1st Sentence", "2nd Sentence","Similarity_Score"]
txt = [0,1,2,3,4]
for num in range(5):
      row = sheet1.row(num)
      for index, col in enumerate(cols):
          value = txt[index] + num
          row.write(index, value)
wb.save("test.xls")
