# CSA09 - Programming in Java | Assignment 3 (CO3)

**Course:** CSA09 - Programming in Java  
**Assignment:** Assignment 3 (CO3: Exception Handling & Multithreading)  
**Mode:** Pair Work (2 Students per Group)  
**Total Marks:** 50  

---


## Repository Structure

```text
CSA09-<RegisterNumber>-Assignment3/
├── README.md                          # Repository overview, build/run steps & summary
├── Assignment3_Complete_Report.md     # Complete Joint Assignment Report (Markdown)
├── Assignment3_Complete_Report.docx   # Formatted Joint Assignment Report (Word Document)
├── .gitignore                         # Ignores compiled .class and temporary output files
├── Q1/                                # Task 1: Bank ATM Simulation (6 Marks)
│   ├── ATMSimulation.java
│   ├── CatchOrderDemo.java
│   ├── UncaughtTraceDemo.java
│   ├── ATMAlternativeErrorCode.java
│   ├── card_reader.txt
│   └── expected_output.txt
├── Q2/                                # Task 2: Online Examination Portal (7 Marks)
│   ├── StorageDemo.java               # [Complete the Code]
│   ├── ExamPortalDemo.java
│   └── expected_output.txt
├── Q3/                                # Task 3: Daily Sales Report Generator (7 Marks)
│   ├── TrackedFileReader.java
│   ├── ManualReportGenerator.java
│   ├── TryWithResourcesReportGenerator.java
│   ├── data/
│   │   ├── store_chennai.txt
│   │   ├── store_bengaluru.txt
│   │   ├── store_mumbai.txt
│   │   └── store_corrupt.txt
│   └── expected_output.txt
├── Q4/                                # Task 4: Parallel Answer-Script Scanning (7 Marks)
│   ├── ScannerStationThreadDemo.java
│   ├── ScannerStationRunnableDemo.java
│   ├── ScannerExecutorAlternative.java
│   └── expected_output.txt
├── Q5/                                # Task 5: Cinema Seat-Booking System (6 Marks)
│   ├── SeatBooking.java               # [Complete the Code]
│   ├── SeatBookingExperiments.java
│   ├── SeatBookingAlternatives.java
│   └── expected_output.txt
├── Q6/                                # Task 6: Hostel Wallet & Mess Token Counter (7 Marks)
│   ├── TokenCounter.java              # [Complete the Code]
│   ├── HostelWalletTransferDemo.java
│   ├── TokenCounterMonitoredDemo.java
│   ├── AlternativesQ6.java
│   └── expected_output.txt
└── Q7/                                # Task 7: Concurrent Semester Result Processor (10 Marks)
    ├── ResultProcessor.java
    ├── ResultProcessorAlternatives.java
    ├── sample_scripts/
    └── expected_output.txt
```

---

## Compilation and Execution Instructions

Open a terminal in the repository root folder and run the commands for each task:

### Q1 — Handling Failure in a Bank ATM Simulation
```bash
cd Q1
javac *.java
java ATMSimulation                 # Runs automated test suite across all 6 paths
java ATMSimulation --interactive   # Optional interactive ATM session
java CatchOrderDemo
java ATMAlternativeErrorCode
cd ..
```

### Q2 — User-Defined Exceptions and Propagation in an Online Examination Portal
```bash
cd Q2
javac *.java
java StorageDemo                   # Complete the Code task
java ExamPortalDemo                # Full 3-layer propagation & chaining demo
cd ..
```

### Q3 — Resource Safety, Debugging and Robust Code
```bash
cd Q3
javac *.java
java ManualReportGenerator         # Demonstrates resource leak on mid-run error
java TryWithResourcesReportGenerator # Demonstrates 0 leaked handles & suppressed exceptions
cd ..
```

### Q4 — Thread Creation and the Thread Life Cycle
```bash
cd Q4
javac *.java
java ScannerStationThreadDemo      # extends Thread + lifecycle + run() vs start()
java ScannerStationRunnableDemo    # implements Runnable + 10 runs + priority + daemon
java ScannerExecutorAlternative    # ExecutorService fixed thread pool alternative
cd ..
```

### Q5 — Race Conditions in a Cinema Seat-Booking System
```bash
cd Q5
javac *.java
java SeatBooking                   # Complete the Code task (Outputs: Seats sold: 10)
java SeatBookingExperiments        # Unsynchronized 10-run race + sync benchmarks
java SeatBookingAlternatives       # AtomicBoolean, ReentrantLock & ConcurrentHashMap
cd ..
```

### Q6 — Deadlock and Inter-Thread Communication
```bash
cd Q6
javac *.java
java TokenCounter                  # Complete the Code task (Outputs: Sum = 210)
java HostelWalletTransferDemo      # Part A: Deadlock reproduction & lock-ordering fix
java TokenCounterMonitoredDemo     # Part B: Buffer size trace, notify vs notifyAll, if vs while
java AlternativesQ6                # ArrayBlockingQueue & ReentrantLock.tryLock alternatives
cd ..
```

### Q7 — Concurrent, Fault-Tolerant Semester Result Processor
```bash
cd Q7
javac *.java
java ResultProcessor               # Hand-checked verification + 10,000-file benchmark (1,2,4,8 threads)
java ResultProcessorAlternatives   # parallelStream() vs Thread-per-file comparison
cd ..
```

---

## Summary of Results

| Task | Topic | Key Outcome / Verified Result |
| :--- | :--- | :--- |
| **Q1** | ATM Exception Handling | Handled `InputMismatchException`, `NumberFormatException`, `InvalidAmountException`, `InsufficientBalanceException`, and `FileNotFoundException` using ordered `catch` & multi-catch blocks; `finally` guaranteed card ejection on every path. |
| **Q2** | Custom Exceptions & Chaining | `StorageDemo` matched exact expected output (`Saved answer01.pdf` / `Error: Could not store answer02.pdf` / `Cause: Disk quota exceeded`). Demonstrated checked vs unchecked propagation across Controller, Service, and Storage layers. |
| **Q3** | Resource Safety & `try-with-resources` | Manual `finally` leaked open file handles (`> 0`) upon mid-batch failure; `try-with-resources` closed all resources in reverse declaration order (`0` leaked handles) and preserved suppressed exceptions. |
| **Q4** | Thread Lifecycle (`Thread` vs `Runnable`) | Verified `NEW -> RUNNABLE -> TIMED_WAITING -> TERMINATED` transitions, proved `run()` executes synchronously on `main`, recorded non-deterministic finish orders across 10 runs, and tested daemon termination. |
| **Q5** | Race Conditions & Synchronization | Unsynchronized booking oversold 10 seats (`11–16` sold); `SeatBooking` with `synchronized` produced `Seats sold: 10` consistently. Benchmarked method-level vs per-seat block synchronization on 10,000 requests. |
| **Q6** | Deadlock & `wait()`/`notifyAll()` | Reproduced circular lock wait (`A -> B` and `B -> A`) and eliminated it via global ID lock ordering. `TokenCounter` produced exact sum `210` across 20 tokens while maintaining `0 <= count <= 5`. |
| **Q7** | Fault-Tolerant Concurrent Processor | Processed 10,000 answer-script files with `ExecutorService`, `Callable`/`Future`, and `ConcurrentHashMap`; isolated missing/corrupt files without aborting the batch and benchmarked scaling across `1, 2, 4, 8` threads. |

---

## Progressive Git Commit Guide (For Submission Compliance)

To satisfy the assignment's progressive commit requirement, commit each task folder separately:
```bash
git init
git add README.md .gitignore
git commit -m "chore: initialize repository structure and README"

git add Q1 && git commit -m "feat(Q1): implement ATM exception handling and error-code alternative"
git add Q2 && git commit -m "feat(Q2): complete StorageDemo and 3-layer exam portal exceptions"
git add Q3 && git commit -m "feat(Q3): implement manual vs try-with-resources sales report generator"
git add Q4 && git commit -m "feat(Q4): implement Thread and Runnable scanner stations with lifecycle demos"
git add Q5 && git commit -m "feat(Q5): complete SeatBooking synchronization and concurrency benchmarks"
git add Q6 && git commit -m "feat(Q6): complete TokenCounter and hostel wallet deadlock prevention"
git add Q7 && git commit -m "feat(Q7): design and benchmark fault-tolerant 10k result processor"
git add Assignment3_Complete_Report.md Assignment3_Complete_Report.docx && git commit -m "docs: add complete joint report and contribution record"
```
