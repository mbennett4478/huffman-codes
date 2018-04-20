if (args.contains('-d')) {
    Huffman h = new Huffman(args[0], args[1])
    h.retrieveHeader()
} else {
    Huffman h = new Huffman(args[0])
    h.generateCounts()
    h.phaseOne()
    h.phaseTwo()
    h.phaseThree()
    h.codeGen()
    h.writeOutCodes()
    h.addToBuffer()
}
