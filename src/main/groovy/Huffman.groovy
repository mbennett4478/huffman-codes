class Huffman {

    String path
    String extension
    def counts = [:]
    ArrayList<Node> nodes = []
    List<Byte> writeBuffer = [] as Byte[]
    List<Boolean> bits = []

    Huffman(String p) {
        path = p

    }

    Huffman(String p, e) {
        path = p
        extension = e
    }

    ArrayList<Integer> findMins () {
        Integer first = null
        Integer second = null
        nodes.eachWithIndex { n, i ->
            if (n.weight != null && n.type != 2) {
                if (first == null)
                    first = i
                else if (nodes[first].weight > n.weight || (nodes[first].weight == n.weight && nodes[first].type > n.type)) {
                    second = first
                    first = i
                } else if (second == null || nodes[second].weight > n.weight || (nodes[second].weight == n.weight && nodes[second].type > n.type)) {
                    second = i
                }
            }
        }

        return [first, second]
    }

    void generateCounts () {
        Byte[] bytes = new File(path).bytes
        bytes.each { b ->
            def n = nodes.find { it.symbol == b }
            if (n)
                n.weight++
            else {
                nodes.push(new Node(b, 1, 0))
            }
        }
        nodes.sort { it.weight }
    }

    void update (Integer i, Integer w) {
        if (nodes[i].type) {
            nodes[i].weight = w
            nodes[i].type = 2
        } else {
            nodes[i].weight = null
            nodes[i].type = null
        }
    }

    void phaseOne () {
        for (int i = 0; i < nodes.size() - 1; i++) {
            def mins = findMins()
            Integer weight = nodes[mins[0]].weight + nodes[mins[1]].weight
            update(mins[0], i)
            update(mins[1], i)
            nodes[i].weight= weight
            nodes[i].type = 1
        }
    }

    void phaseTwo () {
        nodes[nodes.size() - 2].weight = 0
        nodes[nodes.size() - 2].type = 3
        for (int i = nodes.size() - 3; i >= 0; i--) {
            nodes[i].weight = nodes[nodes[i].weight].weight + 1
            nodes[i].type = 3
        }
    }

    void phaseThree () {
        int a = 1
        int u = 0
        int d = 0
        int t = nodes.size() - 2
        int x = nodes.size() - 1
        while (a > 0) {
            while (t >= 0 && nodes[t].weight == d) {
                u++
                t--
            }
            while (a > u) {
                nodes[x].weight = d
                x--
                a--
            }
            a = 2*u
            d++
            u = 0
        }
    }

    void codeGen () {
        nodes[0].code = 0
        for (int i = 1; i < nodes.size(); i++) {
            nodes[i].code = nodes[i-1].code + 1
            int difference = nodes[i-1].weight - nodes[i].weight
            nodes[i].code = nodes[i].code >> difference
        }
    }

    void writeOutCodes () {
        def file = new File('./codeLengths.txt')
        file.createNewFile()
        println "Byte\tLength\tCode"
        nodes.each { n ->
            print "${n.symbol}\t${n.weight}\t"
            for (int i = n.weight - 1; i >= 0 ; i--) {
                int temp = n.code >> i
                if ((temp & 1) == 1)
                    print 1
                else
                    print 0
            }
            print "\n"
            file.newWriter().withWriter {
                it << n.weight
            }
        }

        int sizePartOne = nodes.size() >> 4
        int sizePartTwo = nodes.size & 0x0F
        writeBuffer.push(sizePartOne as Byte)
        writeBuffer.push(sizePartTwo as Byte)
        nodes.each {
            writeBuffer.push(it.symbol)
            writeBuffer.push(it.weight as Byte)
        }
    }

    void addToBuffer () {
        Byte[] bytes = new File(path).bytes
        int byteTracker = 0
        Byte outputB = 0
        bytes.each { b ->
            Node n = nodes.find { it.symbol == b}
            for (int i = n.weight - 1; i >= 0; i--) {
                outputB += ((n.code >> i) & 1) as Byte
                byteTracker++
                if (byteTracker > 7) {
                    byteTracker = 0
                    writeBuffer.push(outputB)
//                    println writeBuffer
                    outputB = 0
                }
                else
                    outputB = outputB << 1
            }
        }
        if (byteTracker != 0) {
            int shiftAmt = 8 - byteTracker
            outputB = outputB << shiftAmt - 1
            writeBuffer.push(outputB)
        }
        writeBuffer.push(byteTracker as Byte)


        File output = new File('./compressed.txt')
        output.createNewFile()
        output.withOutputStream {
            it.write writeBuffer as Byte[]
        }
    }

    void formattedPrint () {
        print "Work: "
        nodes.each {
            if (it.weight != null)
                print "${it.weight}\t"
            else
                print "null\t"
        }
        println ""
    }

    void retrieveHeader () {
        def input = new File(path).bytes as List
        int first = input.remove(0) as int
        int lengthOfHeader = (first << 4) + input.remove(0)
        int backFill = input.remove(input.size() - 1)
        for (int i = 0; i <= (2*lengthOfHeader) - 2; i += 2)
            nodes.add(new Node(input.remove(0), input.remove(0), 0))
        int index = 0
        input.each { b ->
            for (int i = 7; i >= 0; i--) {
                bits[index] = ((b >> i) & 1) == 1
                index++
            }
        }

        codeGen()

        for (int i = 0; i < bits.size() - (8 - backFill + 1); i++) {
            for (Node n : nodes) {
                int temp = 0
                for (int j = i; j < i + n.weight; j++) {
                    temp = temp << 1
                    if (bits[j])
                        temp += 1
                }
                if (temp == n.code) {
                    writeBuffer.push(n.symbol)
                    i += n.weight - 1
                    break
                }
            }
        }

        File output = new File('./output.' + extension)
        output.createNewFile()
        output.withOutputStream {
            it.write writeBuffer as Byte[]
        }
        println writeBuffer
    }

}
