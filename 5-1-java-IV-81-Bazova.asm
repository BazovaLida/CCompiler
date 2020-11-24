.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

foo proc
	push ebp
	mov ebp, esp
	mov eax, [ebp+16]
	mov [ebp-16], eax	;c_var
	mov eax, [ebp+12]
	mov [ebp-12], eax	;b_var
	mov eax, [ebp+8]
	mov [ebp-8], eax	;a_var
;foo_param 
	push [ebp-8]     ;a_val
	push [ebp-12]     ;b_val
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D0
	mov edx, 0ffffffffh
	jmp _D1
_D0:
	mov edx, 0
_D1:
	idiv ECX
	push EAX

	push [ebp-16]     ;c_val
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D2
	mov edx, 0ffffffffh
	jmp _D3
_D2:
	mov edx, 0
_D3:
	idiv ECX
	push EAX

	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret 3

foo endp
func2 proc
	push ebp
	mov ebp, esp
	mov eax, [ebp+12]
	mov [ebp-12], eax	;j_var
	mov eax, [ebp+8]
	mov [ebp-8], eax	;i_var
;func2_param 
	push [ebp-8]     ;i_val
	push [ebp-12]     ;j_val
	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

	pop [ebp-16];	resF2_var
	push [ebp-16]     ;resF2_val
	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret 2

func2 endp
main proc
	push ebp
	mov ebp, esp
	push 100
	pop [ebp-8];	k_var
	push [ebp-8]     ;k_val
	push 5
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D4
	mov edx, 0ffffffffh
	jmp _D5
_D4:
	mov edx, 0
_D5:
	idiv ECX
	push EAX

	pop [ebp-8];	k_var
	push [ebp-8]     ;k_val
	push 23
	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

	pop [ebp-12];	b_var
	push 1
	push [ebp-12]     ;b_val
call func2
	push eax
	pop [ebp-16];	c_var
	push [ebp-8]     ;k_val
	push 4
	push 2
call foo
	push eax
	push [ebp-16]     ;c_val
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D6
	mov edx, 0ffffffffh
	jmp _D7
_D6:
	mov edx, 0
_D7:
	idiv ECX
	push EAX

	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret 0

main ENDP
END start
