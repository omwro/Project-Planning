import utils.Calendar;
import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamConstants;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PPS {

    private static Random randomizer = new Random();

    private String name;                // the name of the planning system refers to its xml source file
    private int planningYear;                   // the year indicates the period of start and end dates of the projects
    private Set<Employee> employees;
    private Set<Project> projects;

    @Override
    public String toString() {
        return String.format("PPS_e%d_p%d", this.employees.size(), this.projects.size());
    }

    private PPS() {
        this.name = "none";
        this.planningYear = 2000;
        this.projects = new TreeSet<>();
        this.employees = new TreeSet<>();
    }

    private PPS(String resourceName, int year) {
        this();
        this.name = resourceName;
        this.planningYear = year;
    }

    /**
     * Reports the statistics of the project planning year
     */
    public void printPlanningStatistics() {
        System.out.printf("\nProject Statistics of '%s' in the year %d\n",
                this.name, this.planningYear);
        if (this.employees == null || this.projects == null ||
                this.employees.size() == 0 || this.projects.size() == 0) {
            System.out.println("No employees or projects have been set up...");
            return;
        }

        System.out.printf("%d employees have been assigned to %d projects:\n\n",
                this.employees.size(), this.projects.size());

        // Average wage of all the employees.
        System.out.printf("1.The average hourly wage of all employees is %.2f\n", this.calculateAverageHourlyWage());

        // The project that has the most working days.
        System.out.printf("2.The longest project is '%s' with %d available working days.\n",
                this.calculateLongestProject().getTitle(), this.calculateLongestProject().getNumWorkingDays());

        // The project with the most people assigned to it.
        int mostProjectAssigned = this.employees.stream().mapToInt(Employee::getAssignedProjectsLength).max().orElse(1);
        System.out.printf("3.The follow employees have the broadest assignment in no less than %d different projects: \n%s\n",
                mostProjectAssigned, this.calculateMostInvolvedEmployees());

        // Total manpower of all projects combined
        System.out.printf("4. The total budget of committed project manpower is %d\n",
                this.calculateTotalManpowerBudget());

        // List of all the junior employees
        System.out.printf("5. Below is an overview of total managed budget by junior employees (hourly wage <= 30): \n%s\n",
                this.calculateManagedBudgetOverview(employee -> employee.getHourlyWage() <= 30));

        // The cumulative monthly spend.
        System.out.printf("6. Below is an overview of cumulative monthly project spends: \n%s\n",
                this.calculateCumulativeMonthlySpends());
    }

    /**
     * calculates the average hourly wage of all known employees in this system
     *
     * @return
     */
    public double calculateAverageHourlyWage() {
        double totalWage = this.employees
                .stream()
                .mapToDouble(Employee::getHourlyWage)
                .sum();
        return totalWage / this.employees.size();
    }

    /**
     * finds the project with the highest number of available working days.
     * (if more than one project with the highest number is found, any one is returned)
     *
     * @return
     */
    public Project calculateLongestProject() {
        return this.projects
                .stream()
                .max(Comparator.comparing(Project::getNumWorkingDays))
                .get();
    }

    /**
     * calculates the total budget for assigned employees across all projects and employees in the system
     * based on the registration of committed hours per day per employee,
     * the number of working days in each project
     * and the hourly rate of each employee
     *
     * @return
     */
    public int calculateTotalManpowerBudget() {
        return this.projects
                .stream()
                .mapToInt(p -> p.getNumWorkingDays() * (p.getCommittedHoursPerDay()
                        .entrySet()
                        .stream()
                        .mapToInt(m -> m.getKey().getHourlyWage() * m.getValue()))
                        .sum())
                .sum();
    }

    /**
     * finds the employees that are assigned to the highest number of different projects
     * (if multiple employees are assigned to the same highest number of projects,
     * all these employees are returned in the set)
     *
     * @return
     */
    public Set<Employee> calculateMostInvolvedEmployees() {
        return this.employees
                .stream()
                .collect(Collectors.groupingBy(
                        Employee::getAssignedProjectsLength,
                        TreeMap::new,
                        Collectors.toSet()
                ))
                .lastEntry()
                .getValue();
    }

    /**
     * Calculates an overview of total managed budget per employee that complies with the filter predicate
     * The total managed budget of an employee is the sum of all man power budgets of all projects
     * that are being managed by this employee
     *
     * @param filter
     * @return
     */
    public Map<Employee, Integer> calculateManagedBudgetOverview(Predicate<Employee> filter) {
        return this.employees
                .stream()
                .filter(filter)
                .collect(Collectors.toMap(employee -> employee,
                        Employee::calculateManagedBudget));
    }

    /**
     * Calculates and overview of total monthly spends across all projects in the system
     * The monthly spend of a single project is the accumulated manpower cost of all employees assigned to the
     * project across all working days in the month.
     *
     * @return
     */
    public Map<Month, Integer> calculateCumulativeMonthlySpends() {
        Map<Month, Integer> mainMap = new TreeMap<>();
        for (Project project : this.projects) {
            int totalWage = 0;
            for (Map.Entry<Employee, Integer> employeeWage : project.getCommittedHoursPerDay().entrySet()) {
                totalWage += employeeWage.getValue() * employeeWage.getKey().getHourlyWage();
            }
            for (LocalDate date : project.getWorkingDays()) {
                // Old Method
                // mainMap.put(date.getMonth(), mainMap.getOrDefault(date.getMonth(), 0) + totalWage);

                // New Method
                mainMap.merge(date.getMonth(), totalWage, Integer::sum);
            }
        }
        return mainMap;
    }

    public String getName() {
        return name;
    }

    /**
     * A builder helper class to compose a small PPS using method-chaining of builder methods
     */
    public static class Builder {
        PPS pps;

        public Builder() {
            this.pps = new PPS();
        }

        /**
         * Add another employee to the PPS being build
         *
         * @param employee
         * @return
         */
        public Builder addEmployee(Employee employee) {
            build().employees.add(employee);
            return this;
        }

        /**
         * Add another project to the PPS
         * register the specified manager as the manager of the new
         *
         * @param project
         * @param manager
         * @return
         */
        public Builder addProject(Project project, Employee manager) {
            Employee currentEmployee = manager;
            build().projects.add(project);

            if (build().employees.contains(manager)) {
                currentEmployee = build().employees.stream()
                        .filter(employee -> employee.equals(manager))
                        .findFirst()
                        .get();
            }

            currentEmployee.getAssignedProjects().add(project);
            currentEmployee.getManagedProjects().add(project);
            build().employees.add(manager);

            return this;
        }

        /**
         * Add a commitment to work hoursPerDay on the project that is identified by projectCode
         * for the employee who is identified by employeeNr
         * This commitment is added to any other commitment that the same employee already
         * has got registered on the same project,
         *
         * @param projectCode
         * @param employeeNr
         * @param hoursPerDay
         * @return
         */
        public Builder addCommitment(String projectCode, int employeeNr, int hoursPerDay) {
            build().projects.forEach(project -> {
                if (project.getCode().equals(projectCode)) {
                    Employee currentEmployee = build().employees.stream().
                            filter(employee -> employee.getNumber() == employeeNr).
                            findFirst().get();
                    project.addCommitment(currentEmployee, hoursPerDay);
                }
            });
            return this;
        }

        /**
         * Complete the PPS being build
         *
         * @return
         */
        public PPS build() {
            return this.pps;
        }
    }

    public Set<Project> getProjects() {
        return this.projects;
    }

    public Set<Employee> getEmployees() {
        return this.employees;
    }

    /**
     * Loads a complete configuration from an XML file
     *
     * @param resourceName the XML file name to be found in the resources folder
     * @return
     */
    public static PPS importFromXML(String resourceName) {
        XMLParser xmlParser = new XMLParser(resourceName);

        try {
            xmlParser.nextTag();
            xmlParser.require(XMLStreamConstants.START_ELEMENT, null, "projectPlanning");
            int year = xmlParser.getIntegerAttributeValue(null, "year", 2000);
            xmlParser.nextTag();

            PPS pps = new PPS(resourceName, year);

            Project.importProjectsFromXML(xmlParser, pps.projects);
            Employee.importEmployeesFromXML(xmlParser, pps.employees, pps.projects);

            return pps;

        } catch (Exception ex) {
            SLF4J.logException("XML error in '" + resourceName + "'", ex);
        }

        return null;
    }
}
