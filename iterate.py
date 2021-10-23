

#re.sub('<[^<]+>', " ", open(fp, 'r', encoding='utf-8').read()
from xml.dom.minidom import parse
import xml.dom.minidom
import os
import re
import sys

def get_all_text( node ):
        if node.nodeType ==  node.TEXT_NODE:
            return node.data+""
        else:
            text_string = ""
            for child_node in node.childNodes:
                text_string += get_all_text( child_node )
            return text_string
#fileList=[];
#dirname = os.path.normpath('C:/Users/pudutta/Desktop')
#parentpath=os.path.normpath("C:/Users/pudutta/Desktop/XML-Add-on-Source")
#for subdir, dirs, files in os.walk(parentpath):
    #for file in files:
        #print os.path.join(subdir, file)
        #filepath = subdir + os.sep + file
        #if filepath.endswith(".dita") or filepath.endswith(".xml") or filepath.endswith(".ditamap"):
            #fileList.append(filepath);

from xml.etree import cElementTree as ET
#element 
cleantext= ""
#with open('/'.join([dirname, 'alldata-id.txt']), 'w', encoding='utf-8') as f:    
    #for idx,fp in enumerate(fileList):
#DOMTree = xml.dom.minidom.parse(sys.argv[1])
DOMTree = xml.dom.minidom.parse(sys.argv[1])
text = get_all_text(DOMTree)
result = ' '.join(text.split());
num_line = "{1}\n".format( 1,  result)
cleantext+=num_line+ "       "

print(cleantext)
        #f.write(num_line)