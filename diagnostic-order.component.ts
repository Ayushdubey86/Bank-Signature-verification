


import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DiagnosticOrderService } from '../../services/diagnostic-order.service';
import { EnglishLabels } from 'src/app/@shared/labels/en-label';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { DiagnosticOrderAddReasonDialogComponent } from '../diagnostic-order-add-reason-dialog/diagnostic-order-add-reason-dialog.component';
import { Subscription } from 'rxjs';
import { DiagnosticOrderPartsNumberComponent } from '../diagnostic-order-parts-number/diagnostic-order-parts-number.component';
import { DiagnosticOrderDialogContentComponent } from '../diagnostic-order-dialog-content/diagnostic-order-dialog-content.component';
@Component({
  selector: 'app-diagnostic-order',
  templateUrl: './diagnostic-order.component.html',
  styleUrls: ['./diagnostic-order.component.scss'],
})
export class DiagnosticOrderComponent implements OnInit {
  hideTable: boolean = false;
  tableName = 'diagnosticOrderTable';
  labels = EnglishLabels;
  selectedRow!: any;
  selectedId!: any;
  firstRowData!: any;
  secondRowData!:any;
  diableSave: boolean = true;
  DiagnosticForm!: FormGroup;
  showTable: boolean = true;
  showForm: boolean = false;
  hideCDO: boolean = true;
  dataSource: any;
  uploadingPoint =["RequestMarket", "pointA", "pointB"]
  disableCreateRule : boolean = true;
  disableApprove: boolean = true;
  createRuleClicked: boolean= false;
  approveState!:boolean;
  partsNumberValue:any[]= [] ;
  value !:any;
  showEditIcon: string | undefined ;
  subscription!:Subscription;
  arrivedData!:any;
  disableDecline: boolean = true;

  constructor(private diagnosticOrderService: DiagnosticOrderService,
   private fb:FormBuilder,private dialog:MatDialog,
   private router: Router,private cdr:ChangeDetectorRef
    ) {
      this.DiagnosticForm = this.fb.group({
        requesterName:['',Validators.required],
        finVin:['',[Validators.pattern(/^[0-9A-Za-z]{17}$/),Validators.required]],
        damageCode:['',Validators.required],
        createdBy:['',Validators.required],
        uploadingPoint:['',Validators.required],
        partNumber:['',Validators.required],
        createdDate:['',Validators.required],
        status:['',Validators.required],
        notes:['', [Validators.maxLength(1000), Validators.pattern("[a-zA-Z0-9,'#@%&* ]*")]]
      })
    }

  ngOnInit(){
        //calling service to fetch the diagnostic order record: 
    this.diagnosticOrderService.getAllDiagnosticOrderData().subscribe( {
      // uncomment the below code once devsphere is ready
      next: (data: any) => {
        if (data) {
          this.dataSource = data;
        }
      },
    error: (err: any) => {
      console.log("error in fetching data from getAllDiagnosticOrder", err)
    }
    })
    
  }

  showDOTable(event: any) {
    this.showTable = event;
    this.hideCDO = true;
  }

  rowClicked(event: any){
    this.disableApprove = true
    const dialogRef = this.dialog.open(DiagnosticOrderDialogContentComponent, {
      width: '513px',
      position: {left: '737px', top: '188px'},
      data: event.id
    });
    dialogRef.afterClosed().subscribe( data => {
      this.value =data.partNumbers.map((item:any)=>`${item.partNumber} | ${item.partName}`).join('\n')
      if(data){
        this.selectedRow = data;
      this.hideTable = true;
      this.showTable = false;
     this.selectedId = data.id;
     this.disableApprove = true
      this.DiagnosticForm?.patchValue({
      requesterName: data.requesterName,
      finVin: data.finVin,
      damageCode: data.damageCode,
      createdBy: data.createdBy,
      uploadingPoint: data.uploadingPoint,
      partNumber: this.value,
      createdDate: data.createdDate,
      status: data.status,
      notes: data.note
      })
      }
      if(data.status !== "APPROVED"){
        this.disableApprove = false;
        this.disableDecline = false;
       
      }
      else if(data.status === "APPROVED"){
        this.disableCreateRule = false;
      }
    })
  }



  backToDOtable(){
    this.hideTable = false
    this.showTable = true;    
  }

createDiagnosticOrder(){
     this.showTable = !this.showTable;
    this.hideCDO = !this.hideCDO;
    this.showForm = !this.showForm;
    this.diagnosticOrderService.updateSecondTableData([]);
  }

  onEdit(item: any, field: string) {
    item.editFieldName = field;
    //once the DO is approved, we should not allow user to save further change or create new rule
   if(this.DiagnosticForm.value.status !== 'APPROVED'){
    this.disableApprove = false;
    this.diableSave = false;
    this.disableDecline = false;
   }
  }

  //edit an existing diagnsotic order
   onSubmit(){
     const newEntry = this.DiagnosticForm.value
     if(this.DiagnosticForm.value.status != 'APPROVED' ){
      this.diagnosticOrderService.updateDoData(this.selectedId ,newEntry).subscribe({
        next: (data:any) => {
          if(data){
            alert('saved successfully')
          }
        },
        error: (err:any)=> {
          console.log("error in updating an existing diagnosticorder", err)
        }
      })
    }  
   }

    approveReason(){
    let dialogObj = {
      id: "DiagnosticOrderAddReasonDialogComponent",
      selectedRowId: this.selectedId,
      action: "approve",
      title:"please enter the reason for approval"
    }
    
    const dialogRef = this.dialog.open(DiagnosticOrderAddReasonDialogComponent, {
       data: dialogObj
    });
    dialogRef.afterClosed().subscribe( data => {
      if(data){
        this.disableApprove = true
        if(data?.status === 'APPROVED'){
          this.disableCreateRule = false;
        }
      }
     
    })
    }


    declineReason(){
      const dialogConfig = new MatDialogConfig();
      let dialogObj = {
        id : "DiagnosticOrderAddReasonDialogComponent",
          selectedRowId: this.selectedId,
          action: "decline",
          title:"please enter the reason for Decline"
      }
      const modalRef= this.dialog.open(DiagnosticOrderAddReasonDialogComponent,{
        data: dialogObj
      });

      modalRef.afterClosed().subscribe( data => {
        if(data.status === 'DECLINED'){
          this.hideTable = false;
          this.showTable = true;
        }
      })
    }

    partsNumberModel(){
      this.diagnosticOrderService.getDiagnosticOrderById(this.selectedId).subscribe({
        next:(data:any)=>{
          if(data){
             this.arrivedData = data.partNumbers;
             this.diagnosticOrderService.updateSecondTableData(this.arrivedData);
             this.cdr.detectChanges();
          }
        }
      })
      let approveStates;

      if(this.DiagnosticForm.value.status == 'APPROVED'){
        approveStates = true;
      }
      else{
        approveStates = false;
      }
      const dialogRef = this.dialog.open(DiagnosticOrderPartsNumberComponent, {
        data:{approveStates},
        
        autoFocus: false,
      });
      dialogRef.afterClosed().subscribe(result => {
        console.log('The dialog was closed', result);
        // Handle the data received from the dialog here and update the main component
        this.partsNumberValue = result;
        this.value = this.partsNumberValue.map((item:any)=>`${item.partNumber} | ${item.partName}`).join('\n');
        this.DiagnosticForm?.patchValue({
          partNumber: this.value,
        })
        
      });
      
    }
    
    navigateTo(){
      this.router.navigate(['/partsRequest']);
      this.diagnosticOrderService.emitTabChange('partsRequest');
      this.diagnosticOrderService.approvedCreateRule = 'partsRequest'
      this.diagnosticOrderService.emitcreateRuleDataToPartsRequest(this.DiagnosticForm.value)
      this.diagnosticOrderService.createRuleFromDO(this.DiagnosticForm.value).subscribe({
        next: (data:any) => {
          if(data){
            //need to check reponse after shreya completes her api work
            console.log("data", data);
            
          }
        },
        error: (err:any)=> {
          console.log("error in creating rule from DO", err)
        }
      })
    }
}