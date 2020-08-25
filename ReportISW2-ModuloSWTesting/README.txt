ISTRUZIONI PER L'ESECUZIONE DEI PROGETTI

BOOKKEEPER:
1) Utilizzare openjdk-11
2) Per fare la build del progetto eseguire:
   - mvn clean install
3) Per eseguire i mutation testing, dovo aper effettuato l'install:
   - mvn org.pitest:pitest-maven:mutationCoverage

TAJO:
1) Utilizzare openjdk-8
2) Per fare la build del progetto eseguire:
   - mvn clean install -Drat.skip
3) Per eseguire i mutation testing, dovo aper effettuato l'install:
   - mvn org.pitest:pitest-maven:mutationCoverage